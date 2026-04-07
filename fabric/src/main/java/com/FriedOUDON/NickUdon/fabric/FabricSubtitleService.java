package com.FriedOUDON.NickUdon.fabric;

import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class FabricSubtitleService {
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

        stand.setCustomName(mod.names().renderLegacy(player, formatted));
        stand.setCustomNameVisible(true);
        updateLocation(stand, player);
    }

    void remove(UUID uuid) {
        ArmorStandEntity stand = displays.remove(uuid);
        if (stand != null && !stand.isRemoved()) {
            stand.discard();
        }
    }

    void shutdown() {
        for (UUID uuid : displays.keySet().toArray(UUID[]::new)) {
            remove(uuid);
        }
    }

    void reload() {
        tickCounter = 0L;
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
