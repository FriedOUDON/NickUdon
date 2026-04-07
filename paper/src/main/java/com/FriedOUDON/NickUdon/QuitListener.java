package com.FriedOUDON.NickUdon;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {
    private final HelloPlugin plugin;
    public QuitListener(HelloPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        plugin.names().clearTeamEntry(e.getPlayer());
        if (plugin.getConfig().getBoolean("displayOverride.onJoinQuit", true)) {
            var p = e.getPlayer();
            String formatted = plugin.names().format(p, p.getName(),
                    plugin.names().getAlias(p.getUniqueId()),
                    plugin.names().getPrefix(p.getUniqueId()));
            var vars = java.util.Map.of("player", formatted);
            String template = plugin.messages().get(p, "quit.announce", vars);
            e.setQuitMessage(ChatColor.translateAlternateColorCodes('&', template));
        }
    }
}
