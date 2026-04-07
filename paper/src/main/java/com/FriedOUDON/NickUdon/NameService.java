package com.FriedOUDON.NickUdon;

import com.FriedOUDON.NickUdon.common.NickFormatter;
import com.FriedOUDON.NickUdon.common.NickProfileRepository;
import com.FriedOUDON.NickUdon.common.PlayerIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.UUID;

public class NameService {
    private final JavaPlugin plugin;
    private final File file;
    private final NickFormatter formatter;
    private final NickProfileRepository profiles;

    public NameService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        migrateLegacyFile();

        PaperConfigAccess config = PaperConfigAccess.pluginConfig(plugin);
        PaperConfigAccess data = PaperConfigAccess.fileConfig(plugin, file);
        this.formatter = new NickFormatter(config);
        this.profiles = new NickProfileRepository(data, config, formatter);
    }

    public void reload() {
        profiles.reload();
    }

    public void setAlias(UUID uuid, String alias) {
        profiles.setAlias(uuid, alias);
    }

    public void clearAlias(UUID uuid) {
        profiles.clearAlias(uuid);
    }

    public String getAlias(UUID uuid) {
        return profiles.getAlias(uuid);
    }

    public void setPrefix(UUID uuid, String prefix) {
        profiles.setPrefix(uuid, prefix);
    }

    public void clearPrefix(UUID uuid) {
        profiles.clearPrefix(uuid);
    }

    public String getPrefix(UUID uuid) {
        return profiles.getPrefix(uuid);
    }

    public void setSubtitle(UUID uuid, String subtitle) {
        profiles.setSubtitle(uuid, subtitle);
    }

    public void clearSubtitle(UUID uuid) {
        profiles.clearSubtitle(uuid);
    }

    public String getSubtitle(UUID uuid) {
        return profiles.getSubtitle(uuid);
    }

    public void setSubtitleEnabled(UUID uuid, boolean enabled) {
        profiles.setSubtitleEnabled(uuid, enabled);
    }

    public boolean isSubtitleEnabled(UUID uuid) {
        return profiles.isSubtitleEnabled(uuid);
    }

    public UUID findAliasConflict(String candidateRaw, UUID self) {
        return profiles.findAliasConflict(candidateRaw, self,
                Bukkit.getOnlinePlayers().stream()
                        .map(player -> new PlayerIdentity(player.getUniqueId(), player.getName()))
                        .toList());
    }

    public void applyDisplay(Player player) {
        String alias = getAlias(player.getUniqueId());
        String prefix = getPrefix(player.getUniqueId());
        boolean bedrockSafe = shouldDownsampleNametag();
        String display = format(player, player.getName(), alias, prefix, bedrockSafe);

        player.setDisplayName(display);
        player.setPlayerListName(display);
        applyNametag(player, alias, prefix, bedrockSafe);

        if (plugin instanceof HelloPlugin hello && hello.subtitles() != null) {
            hello.subtitles().refresh(player);
        }
    }

    public void clearTeamEntry(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getMainScoreboard();
        String name = player.getName();
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith("nick_") && team.hasEntry(name)) {
                team.removeEntry(name);
                if (team.getSize() == 0) team.unregister();
            }
        }
    }

    public String format(Player playerContext, String original, String alias, String prefix) {
        return format(playerContext, original, alias, prefix, false);
    }

    public String format(Player playerContext, String original, String alias, String prefix, boolean bedrockSafe) {
        return formatter.format(original, alias, prefix, bedrockSafe, text -> applyPlaceholders(playerContext, text));
    }

    public String formatChatName(Player playerContext, String original, String alias, String prefix) {
        return formatter.formatChatName(original, alias, prefix, false, text -> applyPlaceholders(playerContext, text));
    }

    public String formatChatNameForViewer(Player playerContext,
                                          Player viewer,
                                          String original,
                                          String alias,
                                          String prefix) {
        boolean bedrockSafe = shouldDownsampleForViewer(viewer);
        return formatter.formatChatName(original, alias, prefix, bedrockSafe, text -> applyPlaceholders(playerContext, text));
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
        return formatter.formatSubtitle(original, alias, prefix, subtitleRaw, bedrockSafe,
                text -> applyPlaceholders(playerContext, text));
    }

    public Component formatSubtitleComponent(Player playerContext,
                                             String original,
                                             String alias,
                                             String prefix,
                                             String subtitleRaw,
                                             boolean bedrockSafe) {
        String legacy = formatSubtitle(playerContext, original, alias, prefix, subtitleRaw, bedrockSafe);
        if (legacy == null || legacy.isBlank()) return Component.empty();

        String ampersand = legacy.replace('\u00A7', '&');
        return LegacyComponentSerializer.legacyAmpersand().deserialize(ampersand);
    }

    public String applyHexColors(String text) {
        return formatter.applyHexColors(text, false);
    }

    public String applyHexColors(String text, boolean bedrockSafe) {
        return formatter.applyHexColors(text, bedrockSafe);
    }

    public boolean hasColorCodes(String text) {
        return formatter.hasColorCodes(text);
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

    public ChatColor getDefaultSubtitleColor() {
        String code = formatter.defaultSubtitleColorCode();
        if (code != null && code.length() >= 2) {
            ChatColor parsed = ChatColor.getByChar(code.charAt(code.length() - 1));
            if (parsed != null) return parsed;
        }
        return ChatColor.AQUA;
    }

    private void migrateLegacyFile() {
        File legacyFile = new File(plugin.getDataFolder(), "aliases.yml");
        if (!file.exists() && legacyFile.exists() && !legacyFile.renameTo(file)) {
            plugin.getLogger().warning("Failed to rename aliases.yml to data.yml");
        }
    }

    private void applyNametag(Player player, String alias, String prefix, boolean bedrockSafe) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getMainScoreboard();
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith("nick_") && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
                if (team.getSize() == 0) team.unregister();
            }
        }

        String uuid = player.getUniqueId().toString().replace("-", "");
        String teamName = ("nick_" + uuid.substring(0, 8) + uuid.substring(uuid.length() - 7));
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        String prefixText = formatter.formatPrefixOnly(prefix, bedrockSafe);
        String suffixText = formatter.formatAliasSuffix(alias, bedrockSafe);

        forceRebroadcastTeam(board, teamName, player.getName(), prefixText, suffixText);

        final Scoreboard boardFinal = board;
        final String teamNameFinal = teamName;
        final String entryFinal = player.getName();
        final String prefixFinal = prefixText;
        final String suffixFinal = suffixText;
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> forceRebroadcastTeam(boardFinal, teamNameFinal, entryFinal, prefixFinal, suffixFinal),
                40L);
    }

    private void forceRebroadcastTeam(Scoreboard board, String teamName, String entry, String prefix, String suffix) {
        Team old = board.getTeam(teamName);
        if (old != null) old.unregister();

        Team team = board.registerNewTeam(teamName);
        team.setPrefix(prefix != null ? prefix : "");
        team.setSuffix(suffix != null ? suffix : "");
        try {
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        } catch (Throwable ignored) {
        }
        team.addEntry(entry);

        if (team.hasEntry(entry)) team.removeEntry(entry);
        Bukkit.getScheduler().runTask(plugin, () -> team.addEntry(entry));
    }

    private boolean hasBedrockOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBedrock(player)) return true;
        }
        return false;
    }

    private boolean isBedrock(Player player) {
        if (player == null) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            for (String methodName : new String[]{"isFloodgatePlayer", "isFloodgateId"}) {
                try {
                    Method method = apiClass.getMethod(methodName, UUID.class);
                    Object result = method.invoke(api, player.getUniqueId());
                    if (result instanceof Boolean bool) return bool;
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().fine("Floodgate reflection failed: " + e.getMessage());
        }
        return false;
    }

    private String applyPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty() || player == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = papi.getMethod("setPlaceholders", Player.class, String.class);
            Object result = method.invoke(null, player, text);
            if (result instanceof String string) return string;
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().fine("PlaceholderAPI setPlaceholders failed: " + e.getMessage());
        }
        return text;
    }
}
