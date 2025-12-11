package com.FriedOUDON.NickUdon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameService {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;
    private static final ChatColor[] LEGACY_CHAT_COLORS = new ChatColor[]{
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
    };
    private static final int[] LEGACY_RGB = new int[]{
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final String DATA_FILE_NAME = "data.yml";
    private static final String ALIASES_ROOT = "aliases";
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[A-Fa-f0-9]{6}");

    public NameService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        migrateLegacyFile();
        this.yml = YamlConfiguration.loadConfiguration(file);
        migrateLegacyAliases();
    }

    private void migrateLegacyFile() {
        File legacyFile = new File(plugin.getDataFolder(), "aliases.yml");
        if (!file.exists() && legacyFile.exists()) {
            if (!legacyFile.renameTo(file)) {
                plugin.getLogger().warning("Failed to rename aliases.yml to data.yml");
            }
        }
    }

    private void migrateLegacyAliases() {
        boolean migrated = false;
        if (yml.getConfigurationSection(ALIASES_ROOT) == null) {
            yml.createSection(ALIASES_ROOT);
        }
        for (String key : new HashSet<>(yml.getKeys(false))) {
            if (ALIASES_ROOT.equals(key)) continue;
            if (!UUID_PATTERN.matcher(key).matches()) continue;

            String value = yml.getString(key);
            yml.set(key, null);
            if (value != null && yml.getString(ALIASES_ROOT + "." + key) == null) {
                yml.set(ALIASES_ROOT + "." + key, toStorageFormat(value));
            }
            migrated = true;
        }
        if (sanitizeSection(ALIASES_ROOT)) migrated = true;
        if (sanitizeSection("prefix")) migrated = true;
        if (sanitizeSection("subtitle")) migrated = true;
        if (migrated) {
            save();
        }
    }

    public void setAlias(UUID uuid, String alias) {
        if (alias == null || alias.isBlank()) {
            yml.set(ALIASES_ROOT + "." + uuid, null);
        } else {
            yml.set(ALIASES_ROOT + "." + uuid, toStorageFormat(alias));
        }
        save();
    }

    public void reload() {
        if (!file.exists()) {
            save();
            return;
        }
        try {
            yml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to reload data.yml: " + e.getMessage());
        }
        migrateLegacyAliases();
    }

    public void clearAlias(UUID uuid) {
        yml.set(ALIASES_ROOT + "." + uuid, null);
        save();
    }

    public String getAlias(UUID uuid) {
        return yml.getString(ALIASES_ROOT + "." + uuid);
    }

    public void applyDisplay(Player p) {
        String original = p.getName();
        String alias = getAlias(p.getUniqueId());
        String prefix = getPrefix(p.getUniqueId());
        boolean bedrockSafe = shouldDownsampleNametag();
        String display = format(p, original, alias, prefix, bedrockSafe);
        p.setDisplayName(display);
        p.setPlayerListName(display);
        applyNametag(p, alias, prefix);
        if (plugin instanceof HelloPlugin hp && hp.subtitles() != null) {
            hp.subtitles().refresh(p);
        }
    }

    private void applyNametag(Player p, String alias, String prefixValue) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getMainScoreboard();

        // Clean up old nick_ teams for this player
        for (Team t : new HashSet<>(board.getTeams())) {
            if (t.getName().startsWith("nick_") && t.hasEntry(p.getName())) {
                t.removeEntry(p.getName());
                if (t.getSize() == 0) t.unregister();
            }
        }

        String u = p.getUniqueId().toString().replace("-", "");
        String teamName = ("nick_" + u.substring(0, 8) + u.substring(u.length() - 7));
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        boolean bedrockSafe = shouldDownsampleNametag();
        PrefixParts prefixParts = formatPrefix(prefixValue, bedrockSafe);
        String prefix = prefixParts.formatted;
        String suffix = "";

        if (alias != null && !alias.isBlank()) {
            String cleanAlias = applyHexColors(ChatColor.translateAlternateColorCodes('&', alias), bedrockSafe);
            String aliasColors = ChatColor.getLastColors(cleanAlias);
            if (aliasColors == null || aliasColors.isEmpty()) aliasColors = getDefaultAliasColor().toString();
            suffix = aliasColors + "[" + cleanAlias + "]" + ChatColor.RESET;
        }

        forceRebroadcastTeam(board, teamName, p.getName(), prefix, suffix);

        final Scoreboard boardFinal = board;
        final String teamNameFinal = teamName;
        final String entryName = p.getName();
        final String prefixFinal = prefix;
        final String suffixFinal = suffix;
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> forceRebroadcastTeam(boardFinal, teamNameFinal, entryName, prefixFinal, suffixFinal),
                40L);
    }

    private void forceRebroadcastTeam(Scoreboard board, String teamName, String entry, String prefix, String suffix) {
        Team old = board.getTeam(teamName);
        if (old != null) old.unregister();

        Team t = board.registerNewTeam(teamName);
        t.setPrefix(prefix != null ? prefix : "");
        t.setSuffix(suffix != null ? suffix : "");
        try {
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        } catch (Throwable ignored) {}
        t.addEntry(entry);

        if (t.hasEntry(entry)) t.removeEntry(entry);
        Bukkit.getScheduler().runTask(plugin, () -> t.addEntry(entry));
    }

    public String format(Player playerContext, String original, String alias, String prefix) {
        return format(playerContext, original, alias, prefix, false);
    }

    public String format(Player playerContext, String original, String alias, String prefix, boolean bedrockSafe) {
        FileConfiguration cfg = plugin.getConfig();
        String fmt = cfg.getString("nameFormat", "%prefix%%original%[%alias%]");
        String fmtNoAlias = cfg.getString("nameFormatNoAlias", "%prefix%%original%");

        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);

        String cleanAlias = alias != null ? applyHexColors(ChatColor.translateAlternateColorCodes('&', alias), bedrockSafe) : "";
        String aliasColors = ChatColor.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) {
            aliasColors = getDefaultAliasColor().toString();
        }
        if (aliasColors == null) aliasColors = "";

        if (cleanAlias.isBlank()) {
            String out = fmtNoAlias.replace("%original%", original)
                    .replace("%alias%", "")
                    .replace("%aliasC%", "")
                    .replace("%prefix%", prefixParts.formatted)
                    .replace("%prefixC%", prefixParts.colors)
                    .replace("%reset%", ChatColor.RESET.toString());
            return applyPlaceholders(playerContext, out);
        }

        String out = fmt.replace("%original%", original)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%prefix%", prefixParts.formatted)
                .replace("%prefixC%", prefixParts.colors)
                .replace("%reset%", ChatColor.RESET.toString());
        return applyPlaceholders(playerContext, out);
    }

    public String formatChatName(Player playerContext, String original, String alias, String prefix) {
        return formatChatNameInternal(playerContext, null, original, alias, prefix, false);
    }

    public String formatChatNameForViewer(Player playerContext, Player viewer, String original, String alias, String prefix) {
        boolean bedrockSafe = shouldDownsampleForViewer(viewer);
        return formatChatNameInternal(playerContext, viewer, original, alias, prefix, bedrockSafe);
    }

    private String formatChatNameInternal(Player playerContext, Player viewer, String original, String alias, String prefix, boolean bedrockSafe) {
        String fmtAlias = plugin.getConfig().getString("chatNameFormat", "%prefix%%aliasC%%alias%%reset%(%original%)");
        String fmtNoAlias = plugin.getConfig().getString("chatNameFormatNoAlias", "%prefix%(%original%)");

        if (alias == null || alias.isBlank() || ChatColor.stripColor(alias).isBlank()) {
            String out = fmtNoAlias.replace("%original%", original)
                    .replace("%prefix%", formatPrefix(prefix, bedrockSafe).formatted)
                    .replace("%alias%", "")
                    .replace("%aliasC%", "")
                    .replace("%reset%", ChatColor.RESET.toString());
            return applyPlaceholders(playerContext, out);
        }

        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);
        String cleanAlias = ChatColor.translateAlternateColorCodes('&', alias);
        cleanAlias = applyHexColors(cleanAlias, bedrockSafe);
        String aliasColors = ChatColor.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) aliasColors = getDefaultAliasColor().toString();
        if (aliasColors == null) aliasColors = "";

        String out = fmtAlias.replace("%original%", original)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%prefix%", prefixParts.formatted)
                .replace("%prefixC%", prefixParts.colors)
                .replace("%reset%", ChatColor.RESET.toString());
        return applyPlaceholders(playerContext, out);
    }

    private ChatColor getDefaultAliasColor() {
        String def = plugin.getConfig().getString("defaultAliasColor", "AQUA");
        try {
            return ChatColor.valueOf(def.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ChatColor.AQUA;
        }
    }

    private ChatColor getDefaultPrefixColor() {
        String def = plugin.getConfig().getString("defaultPrefixColor", plugin.getConfig().getString("defaultAliasColor", "AQUA"));
        try {
            return ChatColor.valueOf(def.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return getDefaultAliasColor();
        }
    }

    private void save() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    // ===== collision check helpers =====

    public String normalizeAlias(String alias) {
        if (alias == null) return "";
        String s = convertHexToLegacy(alias);
        s = ChatColor.stripColor(s);
        if (s == null) s = alias;
        s = s.trim();
        if (plugin.getConfig().getBoolean("aliasUnique.normalizeNFKC", true)) {
            s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        }
        if (plugin.getConfig().getBoolean("aliasUnique.caseInsensitive", true)) {
            s = s.toLowerCase(Locale.ROOT);
        }
        return s;
    }

    public UUID findAliasConflict(String candidateRaw, UUID self) {
        String cand = normalizeAlias(candidateRaw);
        if (cand.isBlank()) return null;
        if (!plugin.getConfig().getBoolean("aliasUnique.enabled", true)) return null;

        ConfigurationSection aliases = yml.getConfigurationSection(ALIASES_ROOT);
        if (aliases != null) {
            for (String key : aliases.getKeys(false)) {
                String raw = aliases.getString(key);
                if (raw == null) continue;
                if (self != null && key.equals(self.toString())) continue;
                if (normalizeAlias(raw).equals(cand)) {
                    try {
                        return UUID.fromString(key);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        if (plugin.getConfig().getBoolean("aliasUnique.denySameAsOriginal", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (self != null && online.getUniqueId().equals(self)) continue;
                if (normalizeAlias(online.getName()).equals(cand)) return online.getUniqueId();
            }
        }
        return null;
    }

    public void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            yml.set("prefix." + uuid, null);
        } else {
            yml.set("prefix." + uuid, toStorageFormat(prefix));
        }
        save();
    }

    public void clearPrefix(UUID uuid) {
        yml.set("prefix." + uuid, null);
        save();
    }

    public String getPrefix(UUID uuid) {
        return yml.getString("prefix." + uuid);
    }

    public void setSubtitle(UUID uuid, String subtitle) {
        if (subtitle == null || subtitle.isBlank()) {
            yml.set("subtitle." + uuid, null);
        } else {
            yml.set("subtitle." + uuid, toStorageFormat(subtitle));
        }
        save();
    }

    public void setSubtitleEnabled(UUID uuid, boolean enabled) {
        yml.set("subtitleEnabled." + uuid, enabled);
        save();
    }

    public void clearSubtitle(UUID uuid) {
        yml.set("subtitle." + uuid, null);
        save();
    }

    public String getSubtitle(UUID uuid) {
        return yml.getString("subtitle." + uuid);
    }

    public boolean isSubtitleEnabled(UUID uuid) {
        return yml.getBoolean("subtitleEnabled." + uuid, true);
    }

    public void clearTeamEntry(Player p) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getMainScoreboard();
        String name = p.getName();
        for (Team t : new HashSet<>(board.getTeams())) {
            if (t.getName().startsWith("nick_") && t.hasEntry(name)) {
                t.removeEntry(name);
                if (t.getSize() == 0) t.unregister();
            }
        }
    }

    private PrefixParts formatPrefix(String rawPrefix) {
        return formatPrefix(rawPrefix, false);
    }

    private PrefixParts formatPrefix(String rawPrefix, boolean bedrockSafe) {
        if (rawPrefix == null || rawPrefix.isBlank()) {
            return new PrefixParts("", "");
        }

        String cleanPrefix = applyHexColors(ChatColor.translateAlternateColorCodes('&', rawPrefix), bedrockSafe);
        String prefixColors = ChatColor.getLastColors(cleanPrefix);
        if ((prefixColors == null || prefixColors.isEmpty()) && !cleanPrefix.isEmpty()) {
            prefixColors = getDefaultPrefixColor().toString();
        }
        if (prefixColors == null) prefixColors = "";

        String fmt = plugin.getConfig().getString("prefixFormat", "%prefixC%%prefix%%reset%");
        String formatted = fmt.replace("%prefix%", cleanPrefix)
                .replace("%prefixC%", prefixColors)
                .replace("%reset%", ChatColor.RESET.toString());
        return new PrefixParts(formatted, prefixColors);
    }

    private PrefixParts applyHexToPrefix(PrefixParts parts) {
        if (parts == null || parts.formatted == null) return parts;
        return new PrefixParts(applyHexColors(parts.formatted), parts.colors);
    }

    public String formatSubtitle(Player playerContext,
                                 String original,
                                 String alias,
                                 String prefix,
                                 String subtitleRaw) {
        return formatSubtitle(playerContext, original, alias, prefix, subtitleRaw, false);
    }

    public String formatSubtitle(Player playerContext,
                                 String original,
                                 String alias,
                                 String prefix,
                                 String subtitleRaw,
                                 boolean bedrockSafe) {
        if (subtitleRaw == null || subtitleRaw.isBlank()) return "";

        String cleanSubtitle = applyHexColors(ChatColor.translateAlternateColorCodes('&', subtitleRaw), bedrockSafe);
        if (ChatColor.stripColor(cleanSubtitle).equals(cleanSubtitle)) {
            cleanSubtitle = getDefaultSubtitleColor() + cleanSubtitle;
        }

        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);
        String cleanAlias = alias != null ? applyHexColors(ChatColor.translateAlternateColorCodes('&', alias), bedrockSafe) : "";
        String aliasColors = ChatColor.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) aliasColors = getDefaultAliasColor().toString();
        if (aliasColors == null) aliasColors = "";

        String fmt = plugin.getConfig().getString("subtitle.format", "%subtitle%");
        String out = fmt.replace("%subtitle%", cleanSubtitle)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%original%", original)
                .replace("%prefix%", prefixParts.formatted)
                .replace("%prefixC%", prefixParts.colors)
                .replace("%reset%", ChatColor.RESET.toString());
        return applyPlaceholders(playerContext, out);
    }

    public String applyHexColors(String text) {
        return applyHexColors(text, false);
    }

    public String applyHexColors(String text, boolean bedrockSafe) {
        if (text == null || text.isBlank()) return text;
        String normalized = normalizeLegacyHex(text);

        if (bedrockSafe && plugin.getConfig().getBoolean("bedrock.hexDownsample", true)) {
            return downsampleHexColors(normalized);
        }

        Matcher m = HEX_COLOR_PATTERN.matcher(normalized);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group();
            try {
                String legacy = hexToLegacy(hex);
                m.appendReplacement(sb, Matcher.quoteReplacement(legacy));
            } catch (IllegalArgumentException e) {
                m.appendReplacement(sb, hex); // leave as-is if invalid
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String hexToLegacy(String hex) {
        // Convert #RRGGBB to legacy §x§R§R§G§G§B§B format
        StringBuilder out = new StringBuilder("§x");
        for (char c : hex.substring(1).toCharArray()) {
            out.append('§').append(c);
        }
        return out.toString();
    }

    private String normalizeLegacyHex(String text) {
        Matcher m = Pattern.compile("§x(§[0-9A-Fa-f]){6}").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String seq = m.group();
            StringBuilder hex = new StringBuilder("#");
            for (int i = 2; i < seq.length(); i += 2) {
                hex.append(seq.charAt(i + 1));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(hex.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String downsampleHexColors(String text) {
        Matcher m = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group();
            ChatColor nearest = nearestLegacy(hex.substring(1));
            if (nearest != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(nearest.toString()));
            } else {
                m.appendReplacement(sb, hex);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private ChatColor nearestLegacy(String rgb) {
        try {
            int r = Integer.parseInt(rgb.substring(0, 2), 16);
            int g = Integer.parseInt(rgb.substring(2, 4), 16);
            int b = Integer.parseInt(rgb.substring(4, 6), 16);
            int best = Integer.MAX_VALUE;
            ChatColor bestColor = ChatColor.WHITE;
            for (int i = 0; i < LEGACY_RGB.length; i++) {
                int lr = (LEGACY_RGB[i] >> 16) & 0xFF;
                int lg = (LEGACY_RGB[i] >> 8) & 0xFF;
                int lb = LEGACY_RGB[i] & 0xFF;
                int dr = r - lr;
                int dg = g - lg;
                int db = b - lb;
                int dist = dr * dr + dg * dg + db * db;
                if (dist < best) {
                    best = dist;
                    bestColor = LEGACY_CHAT_COLORS[i];
                }
            }
            return bestColor;
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean shouldDownsampleForViewer(Player viewer) {
        if (viewer == null) return false;
        if (!plugin.getConfig().getBoolean("bedrock.hexDownsample", true)) return false;
        return isBedrock(viewer);
    }

    public boolean shouldDownsampleNametag() {
        if (!plugin.getConfig().getBoolean("bedrock.hexDownsample", true)) return false;
        if (!plugin.getConfig().getBoolean("bedrock.downsampleNametagWhenBedrockOnline", true)) return false;
        return hasBedrockOnline();
    }

    public boolean shouldDownsampleSubtitle() {
        if (!plugin.getConfig().getBoolean("bedrock.hexDownsample", true)) return false;
        if (!plugin.getConfig().getBoolean("bedrock.downsampleSubtitleWhenBedrockOnline", true)) return false;
        return hasBedrockOnline();
    }

    public boolean hasColorCodes(String text) {
        if (text == null || text.isEmpty()) return false;
        if (HEX_COLOR_PATTERN.matcher(text).find()) return true;
        String legacy = convertHexToLegacy(text);
        return !Objects.equals(ChatColor.stripColor(legacy), legacy);
    }

    private boolean hasBedrockOnline() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isBedrock(online)) return true;
        }
        return false;
    }

    private boolean isBedrock(Player player) {
        if (player == null) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            var getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            for (String methodName : new String[]{"isFloodgatePlayer", "isFloodgateId"}) {
                try {
                    var m = apiClass.getMethod(methodName, UUID.class);
                    Object result = m.invoke(api, player.getUniqueId());
                    if (result instanceof Boolean b) return b;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().fine("Floodgate reflection failed: " + e.getMessage());
        }
        return false;
    }

    public Component formatSubtitleComponent(Player playerContext,
                                             String original,
                                             String alias,
                                             String prefix,
                                             String subtitleRaw,
                                             boolean bedrockSafe) {
        String legacy = formatSubtitle(playerContext, original, alias, prefix, subtitleRaw, bedrockSafe);
        if (legacy == null || legacy.isBlank()) return Component.empty();
        // Use ampersand-based legacy parsing to avoid emitting section signs to the client
        String ampersand = legacy.replace('§', '&');
        return LegacyComponentSerializer.legacyAmpersand().deserialize(ampersand);
    }

    public ChatColor getDefaultSubtitleColor() {
        String def = plugin.getConfig().getString("subtitle.defaultColor",
                plugin.getConfig().getString("defaultSubtitleColor",
                        plugin.getConfig().getString("defaultAliasColor", "AQUA")));
        try {
            return ChatColor.valueOf(def.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return getDefaultAliasColor();
        }
    }

    private record PrefixParts(String formatted, String colors) {}

    private String toStorageFormat(String text) {
        if (text == null) return null;
        return text.replace('§', '&');
    }

    private boolean sanitizeSection(String path) {
        ConfigurationSection sec = yml.getConfigurationSection(path);
        if (sec == null) return false;
        boolean changed = false;
        for (String key : sec.getKeys(false)) {
            Object val = sec.get(key);
            if (val instanceof String s) {
                String clean = toStorageFormat(s);
                if (!Objects.equals(s, clean)) {
                    sec.set(key, clean);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private String convertHexToLegacy(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher m = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(hexToLegacy(m.group())));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String applyPlaceholders(Player p, String text) {
        if (text == null || text.isEmpty()) return text;
        if (p == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            var method = papi.getMethod("setPlaceholders", Player.class, String.class);
            Object result = method.invoke(null, p, text);
            if (result instanceof String s) return s;
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().fine("PlaceholderAPI setPlaceholders failed: " + e.getMessage());
        }
        return text;
    }
}
