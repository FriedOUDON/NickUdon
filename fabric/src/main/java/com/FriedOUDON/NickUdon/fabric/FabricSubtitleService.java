package com.FriedOUDON.NickUdon.fabric;

import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class FabricSubtitleService {
    private static final String MANAGED_TAG = "nickudon_subtitle";
    private static final String OWNER_TAG_PREFIX = "nickudon_subtitle_owner_";
    private static final int PORTAL_COOLDOWN_TICKS = 20 * 60 * 60 * 24 * 365;
    private static final double LEGACY_CLEANUP_RADIUS_XZ = 8.0D;
    private static final double LEGACY_CLEANUP_RADIUS_Y = 10.0D;
    private static final Method SET_MARKER = findArmorStandMethod("setMarker");
    private static final Method SET_SMALL = findArmorStandMethod("setSmall");

    private final NickUdonFabric mod;
    private final Map<UUID, ArmorStandEntity> displays = new HashMap<>();
    private long tickCounter;

    FabricSubtitleService(NickUdonFabric mod) {
        this.mod = mod;
    }

    void tick(MinecraftServer server) {
        long interval = Math.max(1L, mod.config().getLong("subtitle.updateTicks", 4L));
        tickCounter++;
        if (tickCounter % interval != 0) return;

        if (!mod.config().getBoolean("subtitle.enabled", true)) {
            shutdown();
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            refresh(player);
        }
    }

    void refresh(ServerPlayerEntity player) {
        if (player == null || !player.isAlive()) return;
        if (!mod.config().getBoolean("subtitle.enabled", true)) {
            remove(player.getUuid());
            return;
        }
        if (!mod.names().isSubtitleEnabled(player.getUuid())) {
            remove(player.getUuid());
            return;
        }

        String subtitleRaw = mod.names().getSubtitle(player.getUuid());
        String formatted = mod.names().formatSubtitle(
                player.getName().getString(),
                mod.names().getAlias(player.getUuid()),
                mod.names().getPrefix(player.getUuid()),
                subtitleRaw,
                mod.names().shouldDownsampleSubtitle());
        if (formatted == null || formatted.isBlank()) {
            remove(player.getUuid());
            return;
        }

        ArmorStandEntity stand = displays.get(player.getUuid());
        if (stand == null || stand.isRemoved() || stand.getEntityWorld() != player.getEntityWorld()) {
            remove(player.getUuid());
            stand = spawn(player);
            if (stand == null) return;
            displays.put(player.getUuid(), stand);
        }

        prepareManagedStand(stand, player.getUuid());
        stand.setCustomName(mod.names().renderLegacy(player, formatted));
        stand.setCustomNameVisible(true);
        updateLocation(stand, player);
    }

    void remove(UUID uuid) {
        ArmorStandEntity stand = displays.remove(uuid);
        if (stand != null && !stand.isRemoved()) {
            stand.discard();
        }
        discardTaggedStands(uuid);
    }

    void shutdown() {
        for (UUID uuid : displays.keySet().toArray(UUID[]::new)) {
            remove(uuid);
        }
    }

    void reload() {
        tickCounter = 0L;
    }

    void onServerStarted(MinecraftServer server) {
        cleanupManagedStands(server, false);
        displays.clear();
        tickCounter = 0L;
    }

    void onPlayerWorldChange(ServerPlayerEntity player) {
        remove(player.getUuid());
        refresh(player);
    }

    int cleanupLegacyOrphans(ServerCommandSource source) {
        MinecraftServer server = mod.server();
        if (server == null) {
            return 0;
        }

        int removed = cleanupManagedStands(server, true);
        ServerPlayerEntity player = source == null ? null : source.getPlayer();
        if (player != null) {
            removed += cleanupNearbyLegacyStands(player);
        }
        displays.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isRemoved());
        return removed;
    }

    private ArmorStandEntity spawn(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        ArmorStandEntity stand = new ArmorStandEntity(world, player.getX(), player.getY(), player.getZ());
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setCustomNameVisible(true);
        stand.setSilent(true);
        invokeArmorStandBoolean(SET_MARKER, stand, true);
        invokeArmorStandBoolean(SET_SMALL, stand, true);
        prepareManagedStand(stand, player.getUuid());
        updateLocation(stand, player);
        if (!world.spawnEntity(stand)) {
            mod.warn("Failed to spawn subtitle armor stand for " + player.getName().getString());
            return null;
        }
        return stand;
    }

    private void updateLocation(ArmorStandEntity stand, ServerPlayerEntity player) {
        double yOffset = mod.config().getDouble("subtitle.yOffset", 0.35);
        double nameTagOffset = mod.config().getDouble("subtitle.nameTagOffset", 0.25);
        stand.refreshPositionAndAngles(
                player.getX(),
                player.getY() + player.getHeight() + nameTagOffset + yOffset,
                player.getZ(),
                0.0F,
                0.0F);
        stand.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
    }

    private void prepareManagedStand(ArmorStandEntity stand, UUID owner) {
        String ownerTag = ownerTag(owner);
        for (String tag : new ArrayList<>(stand.getCommandTags())) {
            if (tag.startsWith(OWNER_TAG_PREFIX) && !tag.equals(ownerTag)) {
                stand.removeCommandTag(tag);
            }
        }
        stand.addCommandTag(MANAGED_TAG);
        stand.addCommandTag(ownerTag);
        stand.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
    }

    private void discardTaggedStands(UUID owner) {
        MinecraftServer server = mod.server();
        if (server == null) {
            return;
        }

        String ownerTag = ownerTag(owner);
        for (ServerWorld world : server.getWorlds()) {
            for (var entity : world.iterateEntities()) {
                if (entity instanceof ArmorStandEntity stand
                        && stand.getCommandTags().contains(MANAGED_TAG)
                        && stand.getCommandTags().contains(ownerTag)) {
                    stand.discard();
                }
            }
        }
    }

    private int cleanupManagedStands(MinecraftServer server, boolean includeLegacy) {
        int removed = 0;
        for (ServerWorld world : server.getWorlds()) {
            for (var entity : world.iterateEntities()) {
                if (!(entity instanceof ArmorStandEntity stand)) {
                    continue;
                }
                if (!isManagedStand(stand, includeLegacy)) {
                    continue;
                }
                stand.discard();
                removed++;
            }
        }
        return removed;
    }

    private boolean isManagedStand(ArmorStandEntity stand, boolean includeLegacy) {
        if (stand.isRemoved()) {
            return false;
        }
        if (stand.getCommandTags().contains(MANAGED_TAG)) {
            return true;
        }
        return includeLegacy && looksLikeStrictLegacySubtitleStand(stand);
    }

    private int cleanupNearbyLegacyStands(ServerPlayerEntity player) {
        int removed = 0;
        Box box = player.getBoundingBox().expand(LEGACY_CLEANUP_RADIUS_XZ, LEGACY_CLEANUP_RADIUS_Y, LEGACY_CLEANUP_RADIUS_XZ);
        for (ArmorStandEntity stand : player.getEntityWorld().getEntitiesByClass(
                ArmorStandEntity.class,
                box,
                this::looksLikeBroadLegacySubtitleStand)) {
            stand.discard();
            removed++;
        }
        return removed;
    }

    private boolean looksLikeStrictLegacySubtitleStand(ArmorStandEntity stand) {
        return hasSubtitleLikeBaseState(stand)
                && stand.isCustomNameVisible()
                && stand.isInvulnerable()
                && stand.isSilent()
                && stand.isMarker()
                && stand.isSmall()
                && !stand.canHit();
    }

    private boolean looksLikeBroadLegacySubtitleStand(ArmorStandEntity stand) {
        if (!hasSubtitleLikeBaseState(stand)) {
            return false;
        }

        int score = 0;
        if (stand.isCustomNameVisible()) score++;
        if (stand.isInvulnerable()) score++;
        if (stand.isSilent()) score++;
        if (stand.isMarker()) score++;
        if (stand.isSmall()) score++;
        if (!stand.canHit()) score++;
        if (!hasEquipment(stand)) score++;
        if (!stand.hasPassengers()) score++;
        return score >= 4;
    }

    private boolean hasSubtitleLikeBaseState(ArmorStandEntity stand) {
        return stand.hasCustomName()
                && stand.isInvisible()
                && stand.hasNoGravity();
    }

    private boolean hasEquipment(ArmorStandEntity stand) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stand.getEquippedStack(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String ownerTag(UUID owner) {
        return OWNER_TAG_PREFIX + owner.toString().replace("-", "");
    }

    private static Method findArmorStandMethod(String name) {
        try {
            Method method = ArmorStandEntity.class.getDeclaredMethod(name, boolean.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invokeArmorStandBoolean(Method method, ArmorStandEntity stand, boolean value) {
        if (method == null) return;
        try {
            method.invoke(stand, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
