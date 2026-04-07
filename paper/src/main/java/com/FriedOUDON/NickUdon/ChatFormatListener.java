package com.FriedOUDON.NickUdon;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatFormatListener implements Listener {
    private final HelloPlugin plugin;
    public ChatFormatListener(HelloPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chatOverride.enabled", true)) {
            return; // honor config: leave chat formatting untouched
        }

        Player p = event.getPlayer();
        String alias = plugin.names().getAlias(p.getUniqueId());
        String prefix = plugin.names().getPrefix(p.getUniqueId());

        event.renderer((source, displayName, msg, viewer) -> {
            Player viewingPlayer = (viewer instanceof Player) ? (Player) viewer : null;
            String displayNameLegacy = plugin.names().formatChatNameForViewer(p, viewingPlayer, p.getName(), alias, prefix);
            Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(displayNameLegacy);
            return nameComponent.append(Component.text(": ")).append(msg);
        });
    }
}
