package com.FriedOUDON.NickUdon;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SubtitleService implements Listener {
    private final HelloPlugin plugin;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private BukkitTask updater;

    public SubtitleService(HelloPlugin plugin) {
        this.plugin = plugin;
        startUpdater();
    }

    public void refresh(Player p) {
        if (p == null || !p.isOnline()) return;
        if (!plugin.getConfig().getBoolean("subtitle.enabled", true)) {
            remove(p.getUniqueId());
            return;
        }

        if (!plugin.names().isSubtitleEnabled(p.getUniqueId())) {
            remove(p.getUniqueId());
            return;
        }

        String subtitleRaw = plugin.names().getSubtitle(p.getUniqueId());
        boolean bedrockSafe = plugin.names().shouldDownsampleSubtitle();
        Component component = plugin.names().formatSubtitleComponent(
                p,
                p.getName(),
                plugin.names().getAlias(p.getUniqueId()),
                plugin.names().getPrefix(p.getUniqueId()),
                subtitleRaw,
                bedrockSafe);
        if (subtitleRaw == null || subtitleRaw.isBlank() || component == null || component.equals(Component.empty())) {
            remove(p.getUniqueId());
            return;
        }

        TextDisplay display = displays.get(p.getUniqueId());
        if (display == null || display.isDead() || !display.isValid() || !display.getWorld().equals(p.getWorld())) {
            remove(p.getUniqueId());
            display = spawnDisplay(p);
            if (display == null) return;
            displays.put(p.getUniqueId(), display);
        }

        setDisplayText(display, component);
        updateLocation(display, p);
    }

    private TextDisplay spawnDisplay(Player p) {
        try {
            return p.getWorld().spawn(p.getLocation(), TextDisplay.class, td -> {
                td.setBillboard(Display.Billboard.CENTER);
                td.setShadowed(false);
                td.setSeeThrough(false);
                td.setAlignment(TextDisplay.TextAlignment.CENTER);
                td.setPersistent(false);
                td.setInterpolationDuration(1);
                td.setViewRange((float) plugin.getConfig().getDouble("subtitle.viewRange", 48.0));
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to spawn subtitle display for " + p.getName() + ": " + t.getMessage());
            return null;
        }
    }

    private void setDisplayText(TextDisplay display, Component component) {
        if (display == null) return;
        try {
            Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
            var textMethod = display.getClass().getMethod("text", compClass);
            textMethod.invoke(display, component);
            return;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // fall through to string setter
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().fine("TextDisplay text(Component) failed: " + e.getMessage());
        }

        try {
            display.setText(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component));
        } catch (Throwable t) {
            try {
                display.getClass().getMethod("setText", String.class)
                        .invoke(display, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component));
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    private void updateLocation(TextDisplay display, Player p) {
        if (display == null || p == null) return;
        double yOffset = plugin.getConfig().getDouble("subtitle.yOffset", 0.35);
        double nameTagOffset = plugin.getConfig().getDouble("subtitle.nameTagOffset", 0.25);
        Location loc = p.getLocation().clone().add(0, p.getHeight() + nameTagOffset + yOffset, 0);
        display.teleport(loc);
    }

    private void startUpdater() {
        if (updater != null) updater.cancel();
        long period = Math.max(1L, plugin.getConfig().getLong("subtitle.updateTicks", 10L));
        updater = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("subtitle.enabled", true)) {
                for (UUID id : new ArrayList<>(displays.keySet())) remove(id);
                return;
            }
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                refresh(p);
            }
        }, period, period);
    }

    public void remove(UUID uuid) {
        TextDisplay display = displays.remove(uuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    public void shutdown() {
        if (updater != null) updater.cancel();
        for (UUID id : new ArrayList<>(displays.keySet())) remove(id);
    }

    public void reload() {
        startUpdater();
        reloadAll();
    }

    public void reloadAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            refresh(p);
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) { remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onKick(PlayerKickEvent e) { remove(e.getPlayer().getUniqueId()); }
}
