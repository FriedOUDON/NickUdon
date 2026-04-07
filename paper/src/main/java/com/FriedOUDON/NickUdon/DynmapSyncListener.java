package com.FriedOUDON.NickUdon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class DynmapSyncListener implements Listener {
    private final HelloPlugin plugin;
    public DynmapSyncListener(HelloPlugin plugin) { this.plugin = plugin; }

    private void resync(Player p) {
        List<Integer> ticks = plugin.getConfig().getIntegerList("resyncTicks");
        if (ticks == null || ticks.isEmpty()) ticks = java.util.Arrays.asList(0, 40, 60);
        for (int t : ticks) {
            if (t <= 0)
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.names().applyDisplay(p));
            else
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.names().applyDisplay(p), t);
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { resync(e.getPlayer()); }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent e) { resync(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.names().applyDisplay(e.getPlayer()), 1L);
    }
}
