package com.FriedOUDON.NickUdon.fabric;

import com.FriedOUDON.NickUdon.common.NickFormatter;
import com.FriedOUDON.NickUdon.common.NickProfileRepository;
import com.FriedOUDON.NickUdon.common.PlayerIdentity;
import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;

final class FabricNameService {
    private final NickUdonFabric mod;
    private final NickFormatter formatter;
    private final NickProfileRepository profiles;

    FabricNameService(NickUdonFabric mod) {
        this.mod = mod;
        Path dataFile = mod.baseDir().resolve("data.yml");
        FabricYamlConfigAccess data = FabricYamlConfigAccess.fromFile(dataFile, mod::warn);
        this.formatter = new NickFormatter(mod.config());
        this.profiles = new NickProfileRepository(data, mod.config(), formatter);
    }

    void reload() {
        profiles.reload();
    }

    String getAlias(UUID uuid) {
        return profiles.getAlias(uuid);
    }

    void setAlias(UUID uuid, String alias) {
        profiles.setAlias(uuid, alias);
    }

    void clearAlias(UUID uuid) {
        profiles.clearAlias(uuid);
    }

    String getPrefix(UUID uuid) {
        return profiles.getPrefix(uuid);
    }

    void setPrefix(UUID uuid, String prefix) {
        profiles.setPrefix(uuid, prefix);
    }

    void clearPrefix(UUID uuid) {
        profiles.clearPrefix(uuid);
    }

    String getSubtitle(UUID uuid) {
        return profiles.getSubtitle(uuid);
    }

    void setSubtitle(UUID uuid, String subtitle) {
        profiles.setSubtitle(uuid, subtitle);
    }

    void clearSubtitle(UUID uuid) {
        profiles.clearSubtitle(uuid);
    }

    boolean isSubtitleEnabled(UUID uuid) {
        return profiles.isSubtitleEnabled(uuid);
    }

    void setSubtitleEnabled(UUID uuid, boolean enabled) {
        profiles.setSubtitleEnabled(uuid, enabled);
    }

    UUID findAliasConflict(String candidateRaw, UUID self) {
        MinecraftServer server = mod.server();
        if (server == null) return null;

        return profiles.findAliasConflict(candidateRaw, self,
                server.getPlayerManager().getPlayerList().stream()
                        .map(player -> new PlayerIdentity(player.getUuid(), player.getName().getString()))
                        .toList());
    }

    String format(String original, String alias, String prefix, boolean bedrockSafe) {
        return formatter.format(original, alias, prefix, bedrockSafe, null);
    }

    String formatChatName(String original, String alias, String prefix, boolean bedrockSafe) {
        return formatter.formatChatName(original, alias, prefix, bedrockSafe, null);
    }

    String formatSubtitle(String original, String alias, String prefix, String subtitleRaw, boolean bedrockSafe) {
        return formatter.formatSubtitle(original, alias, prefix, subtitleRaw, bedrockSafe, null);
    }

    Text renderLegacy(Object context, String legacy) {
        Text text = FabricLegacyText.parse(legacy);
        try {
            if (context instanceof ServerPlayerEntity player) {
                return Placeholders.parseText(text, PlaceholderContext.of(player));
            }
            if (context instanceof ServerCommandSource source) {
                return Placeholders.parseText(text, PlaceholderContext.of(source));
            }
            if (context instanceof GameProfile profile) {
                MinecraftServer server = mod.server();
                if (server != null) {
                    return Placeholders.parseText(text, PlaceholderContext.of(profile, server));
                }
            }
            if (context instanceof MinecraftServer server) {
                return Placeholders.parseText(text, PlaceholderContext.of(server));
            }
        } catch (Throwable ignored) {
        }
        return text;
    }

    String applyHexColors(String text, boolean bedrockSafe) {
        return formatter.applyHexColors(text, bedrockSafe);
    }

    String colorize(String text, boolean bedrockSafe) {
        return formatter.colorize(text, bedrockSafe);
    }

    boolean hasColorCodes(String text) {
        return formatter.hasColorCodes(text);
    }

    String defaultAliasColorCode() {
        return formatter.defaultAliasColorCode();
    }

    String defaultPrefixColorCode() {
        return formatter.defaultPrefixColorCode();
    }

    String defaultSubtitleColorCode() {
        return formatter.defaultSubtitleColorCode();
    }

    boolean shouldDownsampleForViewer(ServerPlayerEntity viewer) {
        return false;
    }

    boolean shouldDownsampleNametag() {
        return false;
    }

    boolean shouldDownsampleSubtitle() {
        return false;
    }

    void applyDisplay(ServerPlayerEntity player) {
        MinecraftServer server = mod.server();
        if (server == null) return;

        ServerScoreboard board = server.getScoreboard();
        String entry = player.getNameForScoreboard();

        clearTeamEntry(player);

        String uuid = player.getUuidAsString().replace("-", "");
        String teamName = "nick_" + uuid.substring(0, 8) + uuid.substring(uuid.length() - 7);
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team existing = board.getTeam(teamName);
        if (existing != null) board.removeTeam(existing);

        Team team = board.addTeam(teamName);
        team.setPrefix(renderLegacy(player,
                formatter.formatPrefixOnly(getPrefix(player.getUuid()), shouldDownsampleNametag())));
        team.setSuffix(renderLegacy(player,
                formatter.formatAliasSuffix(getAlias(player.getUuid()), shouldDownsampleNametag())));
        team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        board.addScoreHolderToTeam(entry, team);
    }

    void clearTeamEntry(ServerPlayerEntity player) {
        MinecraftServer server = mod.server();
        if (server == null) return;

        ServerScoreboard board = server.getScoreboard();
        String entry = player.getNameForScoreboard();

        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith("nick_") && team.getPlayerList().contains(entry)) {
                board.clearTeam(entry);
                if (team.getPlayerList().isEmpty()) {
                    board.removeTeam(team);
                }
            }
        }
    }
}
