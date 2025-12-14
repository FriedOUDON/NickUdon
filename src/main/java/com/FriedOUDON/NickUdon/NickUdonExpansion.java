package com.FriedOUDON.NickUdon;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NickUdonExpansion extends PlaceholderExpansion {
    private final HelloPlugin plugin;

    public NickUdonExpansion(HelloPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "nickudon"; }

    @Override
    public @NotNull String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player p, @NotNull String params) {
        if (p == null) return "";
        String alias = plugin.names().getAlias(p.getUniqueId());
        String prefix = plugin.names().getPrefix(p.getUniqueId());
        boolean bedrockSafe = plugin.names().shouldDownsampleNametag();
        String lower = params.toLowerCase();

        return switch (lower) {
            case "alias" -> nullToEmpty(alias);
            case "alias_stripped" -> ChatColor.stripColor(nullToEmpty(alias));
            case "prefix" -> nullToEmpty(prefix);
            case "display" -> plugin.names().format(p, p.getName(), alias, prefix, bedrockSafe);
            case "display_no_prefix" -> plugin.names().format(p, p.getName(), alias, "", bedrockSafe);
            case "name" -> p.getName();
            default -> null;
        };
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
