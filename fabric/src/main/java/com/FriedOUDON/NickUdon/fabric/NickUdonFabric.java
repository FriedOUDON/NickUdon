package com.FriedOUDON.NickUdon.fabric;

import com.FriedOUDON.NickUdon.common.LegacyTextUtil;
import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class NickUdonFabric implements ModInitializer {
    public static final String MOD_ID = "nickudon";

    private final Logger logger = LoggerFactory.getLogger(MOD_ID);
    private final FabricConsoleOutput console = new FabricConsoleOutput();

    private FabricYamlConfigAccess config;
    private FabricNameService names;
    private FabricMessageService messages;
    private FabricSubtitleService subtitles;
    private FabricCommandHandler commands;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        ensureDefaults();

        this.config = FabricYamlConfigAccess.fromFile(baseDir().resolve("config.yml"), this::warn);
        mergeBundledConfigDefaults();
        this.names = new FabricNameService(this);
        this.messages = new FabricMessageService(this);
        this.subtitles = new FabricSubtitleService(this);
        this.commands = new FabricCommandHandler(this);

        registerLifecycle();
        registerEvents();
        registerCommands();
        registerPlaceholders();
    }

    Path baseDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }

    FabricYamlConfigAccess config() {
        return config;
    }

    FabricNameService names() {
        return names;
    }

    FabricMessageService messages() {
        return messages;
    }

    FabricSubtitleService subtitles() {
        return subtitles;
    }

    MinecraftServer server() {
        return server;
    }

    FabricConsoleOutput console() {
        return console;
    }

    boolean isLocalConsole(ServerCommandSource source) {
        if (source == null || source.isExecutedByPlayer() || source.getEntity() != null) {
            return false;
        }

        ServerCommandSource consoleSource = source.getServer().getCommandSource();
        return source == consoleSource
                || (source.getName().equals(consoleSource.getName())
                && source.getDisplayName().getString().equals(consoleSource.getDisplayName().getString()));
    }

    void reload() {
        config.load();
        names.reload();
        messages.reload();
        subtitles.reload();

        if (server == null) return;
        subtitles.onServerStarted(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            names.applyDisplay(player);
            subtitles.refresh(player);
        }
    }

    boolean hasPermission(ServerCommandSource source, String permission) {
        return NickUdonPermissions.check(source, permission);
    }

    boolean hasPermission(ServerPlayerEntity player, String permission) {
        return NickUdonPermissions.check(player, permission);
    }

    Reader openBundledText(String resourcePath) {
        InputStream stream = NickUdonFabric.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) return null;
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    void warn(String message) {
        logger.warn(message);
    }

    private void registerLifecycle() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            NickUdonPermissions.primeKnownNodes(server);
            subtitles.onServerStarted(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            subtitles.shutdown();
            this.server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> subtitles.tick(server));
    }

    private void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            names.applyDisplay(player);
            subtitles.refresh(player);

            String formatted = names.format(player.getName().getString(), names.getAlias(player.getUuid()), names.getPrefix(player.getUuid()), false);
            player.sendMessage(names.renderLegacy(player, messages.get(player, "join.welcome", Map.of("player", formatted))), false);

            if (config.getBoolean("displayOverride.onJoinQuit", true)) {
                broadcastToAll(messages.get(player, "join.announce", Map.of("player", formatted)));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            names.clearTeamEntry(player);
            subtitles.remove(player.getUuid());

            if (config.getBoolean("displayOverride.onJoinQuit", true)) {
                String formatted = names.format(player.getName().getString(), names.getAlias(player.getUuid()), names.getPrefix(player.getUuid()), false);
                broadcastToAll(messages.get(player, "quit.announce", Map.of("player", formatted)));
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            names.applyDisplay(newPlayer);
            subtitles.refresh(newPlayer);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            names.applyDisplay(player);
            subtitles.onPlayerWorldChange(player);
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!config.getBoolean("chatOverride.enabled", true)) return true;

            String alias = names.getAlias(sender.getUuid());
            String prefix = names.getPrefix(sender.getUuid());
            String displayName = names.formatChatName(sender.getName().getString(), alias, prefix, false);
            Text text = names.renderLegacy(sender, displayName + LegacyTextUtil.RESET + ": ")
                    .copy()
                    .append(Text.literal(message.getSignedContent()));

            MinecraftServer current = sender.getCommandSource().getServer();
            for (ServerPlayerEntity viewer : current.getPlayerManager().getPlayerList()) {
                viewer.sendMessage(text, false);
            }
            console.sendInfo(current, text);
            return false;
        });

        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) ->
                !(config.getBoolean("displayOverride.onJoinQuit", true) && isJoinQuitMessage(message)));
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> commands.register(dispatcher));
    }

    private void registerPlaceholders() {
        registerPlaceholder("alias", (profile, player) -> names.getAlias(profile.id()));
        registerPlaceholder("alias_stripped", (profile, player) -> {
            String alias = names.getAlias(profile.id());
            return alias == null ? "" : alias.replaceAll("(?i)\u00A7x(\u00A7[0-9a-f]){6}|\u00A7[0-9A-FK-OR]|&[0-9A-FK-OR]", "");
        });
        registerPlaceholder("prefix", (profile, player) -> names.getPrefix(profile.id()));
        registerPlaceholder("display", (profile, player) ->
                names.format(profile.name(), names.getAlias(profile.id()), names.getPrefix(profile.id()), false));
        registerPlaceholder("display_no_prefix", (profile, player) ->
                names.format(profile.name(), names.getAlias(profile.id()), "", false));
        registerPlaceholder("chat", (profile, player) ->
                names.formatChatName(profile.name(), names.getAlias(profile.id()), names.getPrefix(profile.id()), false));
        registerPlaceholder("name", (profile, player) -> profile.name());
    }

    private void registerPlaceholder(String name, PlaceholderValue provider) {
        Placeholders.register(Identifier.of(MOD_ID, name), (ctx, arg) -> {
            if (!ctx.hasGameProfile()) return PlaceholderResult.invalid("No profile");

            GameProfile profile = ctx.gameProfile();
            ServerPlayerEntity player = ctx.hasPlayer() ? ctx.player() : null;
            String value = provider.get(profile, player);
            if (value == null) value = "";
            return PlaceholderResult.value(names.renderLegacy(player != null ? player : profile, value));
        });
    }

    private void ensureDefaults() {
        try {
            Files.createDirectories(baseDir().resolve("lang"));
            copyDefault("config.yml");
            copyDefault("lang/en_US.yml");
            copyDefault("lang/ja_JP.yml");
        } catch (IOException e) {
            warn("Failed to initialize config directory: " + e.getMessage());
        }
    }

    private void copyDefault(String resource) throws IOException {
        Path target = baseDir().resolve(resource);
        if (Files.exists(target)) return;

        try (InputStream input = NickUdonFabric.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) return;
            Files.createDirectories(target.getParent());
            Files.copy(input, target);
        }
    }

    private void mergeBundledConfigDefaults() {
        try (Reader reader = openBundledText("config.yml")) {
            if (reader == null) {
                return;
            }
            FabricYamlConfigAccess defaults = FabricYamlConfigAccess.fromReader(reader, this::warn);
            if (config.mergeMissing(defaults)) {
                config.save();
            }
        } catch (IOException e) {
            warn("Failed to merge bundled config defaults: " + e.getMessage());
        }
    }

    private void broadcastToAll(String legacy) {
        if (server == null) return;

        Text text = names.renderLegacy(server, legacy);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(text, false);
        }
        console.sendInfo(server, text);
    }

    private boolean isJoinQuitMessage(Text message) {
        if (!(message.getContent() instanceof TranslatableTextContent content)) return false;
        String key = content.getKey();
        return "multiplayer.player.joined".equals(key)
                || "multiplayer.player.joined.renamed".equals(key)
                || "multiplayer.player.left".equals(key);
    }

    @FunctionalInterface
    private interface PlaceholderValue {
        String get(GameProfile profile, ServerPlayerEntity player);
    }
}
