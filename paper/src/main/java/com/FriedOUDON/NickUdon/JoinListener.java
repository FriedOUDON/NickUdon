package com.FriedOUDON.NickUdon;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

public class JoinListener implements Listener {
    private final HelloPlugin plugin;
    public JoinListener(HelloPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Map<String,String> vars = new HashMap<>();
        var p = e.getPlayer();
        String formatted = plugin.names().format(p, p.getName(),
                plugin.names().getAlias(p.getUniqueId()),
                plugin.names().getPrefix(p.getUniqueId()));
        vars.put("player", formatted);
        p.sendMessage(ChatColor.AQUA + plugin.messages().get(p, "join.welcome", vars));
        plugin.names().applyDisplay(p);

        if (plugin.getConfig().getBoolean("displayOverride.onJoinQuit", true)) {
            String template = plugin.messages().get(p, "join.announce", vars);
            e.setJoinMessage(ChatColor.translateAlternateColorCodes('&', template));
        }
    }
}
