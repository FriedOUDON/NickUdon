package com.FriedOUDON.NickUdon.fabric;

import com.FriedOUDON.NickUdon.common.LegacyTextUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class FabricCommandHandler {
    private final NickUdonFabric mod;

    FabricCommandHandler(NickUdonFabric mod) {
        this.mod = mod;
    }

    void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> primary = registerLiteral(dispatcher, "nickudon");
        registerLiteral(dispatcher, "name");

        for (String alias : configuredAliases()) {
            if (alias == null || alias.isBlank()) continue;
            String normalized = alias.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("nickudon") || normalized.equals("name")) continue;
            dispatcher.register(CommandManager.literal(normalized)
                    .requires(source -> mod.hasPermission(source, "nickudon.use"))
                    .redirect(primary));
        }
    }

    private LiteralCommandNode<ServerCommandSource> registerLiteral(CommandDispatcher<ServerCommandSource> dispatcher, String literal) {
        return dispatcher.register(CommandManager.literal(literal)
                .requires(source -> mod.hasPermission(source, "nickudon.use"))
                .executes(ctx -> execute(ctx.getSource(), literal, ""))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggest(ctx.getSource(), literal, builder))
                        .executes(ctx -> execute(ctx.getSource(), literal, StringArgumentType.getString(ctx, "args")))));
    }

    private List<String> configuredAliases() {
        List<String> aliases = mod.config().getStringList("commandAliases");
        return aliases.isEmpty() ? List.of("nu") : aliases;
    }

    private CompletableFuture<Suggestions> suggest(ServerCommandSource source, String literal, SuggestionsBuilder builder) {
        CompletionContext completion = CompletionContext.from(builder);
        if ("name".equalsIgnoreCase(literal)) {
            return suggestName(source, completion);
        }
        return suggestMain(source, completion);
    }

    private CompletableFuture<Suggestions> suggestMain(ServerCommandSource source, CompletionContext completion) {
        if (completion.index() == 0) {
            return completion.suggest(List.of("reload", "name", "nick", "alias", "rename", "prefix", "subtitle", "lang"));
        }

        String sub = completion.token(0).toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "lang" -> suggestLang(completion);
            case "name", "nick", "alias", "rename" -> suggestAlias(source, completion, false);
            case "prefix" -> suggestPrefix(source, completion);
            case "subtitle" -> suggestSubtitle(source, completion);
            default -> Suggestions.empty();
        };
    }

    private CompletableFuture<Suggestions> suggestName(ServerCommandSource source, CompletionContext completion) {
        return suggestAlias(source, completion, true);
    }

    private CompletableFuture<Suggestions> suggestLang(CompletionContext completion) {
        if (completion.index() != 1) {
            return Suggestions.empty();
        }
        return completion.suggest(List.of("en_US", "ja_JP"));
    }

    private CompletableFuture<Suggestions> suggestAlias(ServerCommandSource source,
                                                        CompletionContext completion,
                                                        boolean shorthand) {
        int index = completion.index();
        boolean canEditOthers = mod.hasPermission(source, "nickudon.nickname.others") || mod.hasPermission(source, "nickudon.admin");

        if (index == 0 || (!shorthand && index == 1)) {
            List<String> suggestions = new ArrayList<>();
            if (canEditOthers) {
                suggestions.addAll(playerSuggestions(source));
            }
            suggestions.add("clear");
            return completion.suggest(suggestions);
        }

        String targetName = completion.token(index - 1);
        if (index == (shorthand ? 1 : 2) && canEditOthers && isOtherOnlineTarget(source, targetName)) {
            return completion.suggest(List.of("clear"));
        }

        return Suggestions.empty();
    }

    private CompletableFuture<Suggestions> suggestPrefix(ServerCommandSource source, CompletionContext completion) {
        int index = completion.index();
        boolean canEditOthers = !(source.getEntity() instanceof ServerPlayerEntity)
                || mod.hasPermission(source, "nickudon.prefix.others")
                || mod.hasPermission(source, "nickudon.admin");

        if (index == 1) {
            List<String> suggestions = new ArrayList<>();
            if (canEditOthers) {
                suggestions.addAll(playerSuggestions(source));
            }
            suggestions.add("clear");
            return completion.suggest(suggestions);
        }

        if (index == 2 && canEditOthers && isOtherOnlineTarget(source, completion.token(1))) {
            return completion.suggest(List.of("clear"));
        }

        return Suggestions.empty();
    }

    private CompletableFuture<Suggestions> suggestSubtitle(ServerCommandSource source, CompletionContext completion) {
        int index = completion.index();
        boolean canEditOthers = !(source.getEntity() instanceof ServerPlayerEntity)
                || mod.hasPermission(source, "nickudon.subtitle.others")
                || mod.hasPermission(source, "nickudon.admin");

        List<String> actions = List.of("clear", "on", "off", "enable", "disable");
        if (index == 1) {
            List<String> suggestions = new ArrayList<>();
            if (canEditOthers) {
                suggestions.addAll(playerSuggestions(source));
            }
            suggestions.addAll(actions);
            return completion.suggest(suggestions);
        }

        if (index == 2 && canEditOthers && isOtherOnlineTarget(source, completion.token(1))) {
            return completion.suggest(actions);
        }

        return Suggestions.empty();
    }

    private List<String> playerSuggestions(ServerCommandSource source) {
        List<String> out = new ArrayList<>();
        ServerPlayerEntity self = source.getPlayer();
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (self != null && player.getUuid().equals(self.getUuid())) continue;
            out.add(player.getName().getString());
        }
        return out;
    }

    private boolean isOtherOnlineTarget(ServerCommandSource source, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(name);
        if (target == null) {
            return false;
        }

        ServerPlayerEntity self = source.getPlayer();
        return self == null || !target.getUuid().equals(self.getUuid());
    }

    private int execute(ServerCommandSource source, String label, String rawArgs) {
        if (!mod.hasPermission(source, "nickudon.use")) {
            sendError(source, msg(source, "no-permission", null));
            return 0;
        }

        String[] args = rawArgs == null || rawArgs.isBlank() ? new String[0] : rawArgs.split(" ");

        if ("name".equalsIgnoreCase(label)) {
            String[] rewritten = new String[args.length + 1];
            rewritten[0] = "name";
            System.arraycopy(args, 0, rewritten, 1, args.length);
            args = rewritten;
            label = "nickudon";
        }

        if (args.length == 0) {
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                send(source, mod.messages().get(player, "hello.self", Map.of("player", player.getName().getString())));
            } else {
                send(source, mod.messages().get(mod.messages().defaultLocale(), "hello.console", null));
            }
            return 1;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            if (!mod.hasPermission(source, "nickudon.admin")) {
                sendError(source, msg(source, "no-permission", null));
                return 0;
            }
            mod.reload();
            send(source, msg(source, "reloaded", null));
            return 1;
        }

        if ("lang".equals(sub)) {
            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                sendError(source, msg(source, "errors.players-only", null));
                return 0;
            }
            if (args.length < 2) {
                sendError(source, msg(source, "errors.usage-lang", Map.of("label", label)));
                return 0;
            }
            String code = args[1];
            mod.messages().setPlayerLocaleOverride(player.getUuid(), code);
            send(player, mod.messages().get(player, "current-locale", Map.of("locale", code)));
            return 1;
        }

        return switch (sub) {
            case "name", "nick", "alias", "rename" -> handleAlias(source, label, sub, args);
            case "prefix" -> handlePrefix(source, label, args);
            case "subtitle" -> handleSubtitle(source, label, args);
            default -> {
                sendError(source, msg(source, "unknown-subcommand", null));
                yield 0;
            }
        };
    }

    private int handleAlias(ServerCommandSource source, String label, String sub, String[] args) {
        if (!mod.hasPermission(source, "nickudon.nickname")) {
            sendError(source, msg(source, "no-permission", null));
            return 0;
        }
        if (args.length < 2) {
            sendError(source, msg(source, "errors.usage-name", Map.of("label", label, "sub", sub)));
            return 0;
        }

        boolean canEditOthers = mod.hasPermission(source, "nickudon.nickname.others") || mod.hasPermission(source, "nickudon.admin");
        TargetRef target = null;
        int aliasStartIndex = 1;
        ServerPlayerEntity self = source.getEntity() instanceof ServerPlayerEntity player ? player : null;

        if (args.length >= 3 && canEditOthers) {
            TargetRef candidate = findTarget(source.getServer(), args[1]);
            if (candidate != null && (self == null || !candidate.uuid().equals(self.getUuid()))) {
                target = candidate;
                aliasStartIndex = 2;
            }
        }

        if (target == null) {
            if (self == null) {
                sendError(source, msg(source, "errors.specify-player", null));
                return 0;
            }
            target = new TargetRef(self.getUuid(), self.getName().getString(), self);
        }

        if (args.length <= aliasStartIndex) {
            sendError(source, msg(source, "errors.usage-name", Map.of("label", label, "sub", sub)));
            return 0;
        }

        if ("clear".equalsIgnoreCase(args[aliasStartIndex])) {
            mod.names().clearAlias(target.uuid());
            if (target.online() != null) {
                mod.names().applyDisplay(target.online());
                mod.names().clearTeamEntry(target.online());
            }
            if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
                send(target.online(), mod.messages().get(target.online(), "nickname.cleared-by", Map.of("actor", source.getName())));
            }
            if (self != null && self.getUuid().equals(target.uuid())) {
                send(source, msg(source, "nickname.cleared-self", null));
            } else {
                send(source, msg(source, "nickname.cleared-other", Map.of("target", target.name())));
            }
            broadcastChange("alias", source, target, null, "cleared");
            return 1;
        }

        String alias = String.join(" ", Arrays.copyOfRange(args, aliasStartIndex, args.length));
        if (alias.length() > 32) alias = alias.substring(0, 32);
        alias = LegacyTextUtil.translateAlternateColorCodes('&', alias);
        if (!mod.names().hasColorCodes(alias)) {
            alias = mod.names().defaultAliasColorCode() + alias;
        }

        UUID conflict = mod.names().findAliasConflict(alias, target.uuid());
        if (conflict != null) {
            String other = target.name();
            ServerPlayerEntity conflictPlayer = source.getServer().getPlayerManager().getPlayer(conflict);
            if (conflictPlayer != null) other = conflictPlayer.getName().getString();
            sendError(source, msg(source, "nickname.conflict", Map.of("other", other)));
            return 0;
        }

        mod.names().setAlias(target.uuid(), alias);
        if (target.online() != null) mod.names().applyDisplay(target.online());

        String formatted = mod.names().format(target.name(), alias, mod.names().getPrefix(target.uuid()), false);
        if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
            send(target.online(), mod.messages().get(target.online(), "nickname.set-by-other",
                    Map.of("actor", source.getName(), "formatted", formatted)));
            send(source, msg(source, "nickname.set-other", Map.of("target", target.name(), "formatted", formatted)));
        } else {
            send(source, msg(source, "nickname.set-self", Map.of("formatted", formatted)));
        }
        broadcastChange("alias", source, target, formatted, "set");
        return 1;
    }

    private int handlePrefix(ServerCommandSource source, String label, String[] args) {
        boolean isConsole = !(source.getEntity() instanceof ServerPlayerEntity);
        if (!isConsole && !(mod.hasPermission(source, "nickudon.prefix") || mod.hasPermission(source, "nickudon.admin"))) {
            sendError(source, msg(source, "no-permission", null));
            return 0;
        }
        if (args.length < 2) {
            sendError(source, msg(source, "errors.usage-prefix", Map.of("label", label, "sub", "prefix")));
            return 0;
        }

        ServerPlayerEntity self = source.getEntity() instanceof ServerPlayerEntity player ? player : null;
        boolean canEditOthers = isConsole || mod.hasPermission(source, "nickudon.prefix.others") || mod.hasPermission(source, "nickudon.admin");
        TargetRef target = null;
        int startIndex = 1;
        if (args.length >= 3 && canEditOthers) {
            TargetRef candidate = findTarget(source.getServer(), args[1]);
            if (candidate != null && (self == null || !candidate.uuid().equals(self.getUuid()))) {
                target = candidate;
                startIndex = 2;
            }
        }

        if (target == null) {
            if (self == null) {
                sendError(source, msg(source, "errors.specify-player", null));
                return 0;
            }
            target = new TargetRef(self.getUuid(), self.getName().getString(), self);
        }

        if (args.length <= startIndex) {
            sendError(source, msg(source, "errors.usage-prefix", Map.of("label", label, "sub", "prefix")));
            return 0;
        }

        if ("clear".equalsIgnoreCase(args[startIndex])) {
            mod.names().clearPrefix(target.uuid());
            if (target.online() != null) {
                mod.names().applyDisplay(target.online());
                mod.names().clearTeamEntry(target.online());
            }
            if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
                send(target.online(), mod.messages().get(target.online(), "prefix.cleared-by", Map.of("actor", source.getName())));
            }
            if (self != null && self.getUuid().equals(target.uuid())) {
                send(source, msg(source, "prefix.cleared-self", null));
            } else {
                send(source, msg(source, "prefix.cleared-other", Map.of("target", target.name())));
            }
            broadcastChange("prefix", source, target, null, "cleared");
            return 1;
        }

        String prefix = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        if (prefix.length() > 32) prefix = prefix.substring(0, 32);
        prefix = LegacyTextUtil.translateAlternateColorCodes('&', prefix);
        if (!mod.names().hasColorCodes(prefix)) {
            prefix = mod.names().defaultPrefixColorCode() + prefix;
        }

        mod.names().setPrefix(target.uuid(), prefix);
        if (target.online() != null) mod.names().applyDisplay(target.online());

        String formatted = mod.names().format(target.name(), mod.names().getAlias(target.uuid()), prefix, false);
        if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
            send(target.online(), mod.messages().get(target.online(), "prefix.set-by-other",
                    Map.of("actor", source.getName(), "formatted", formatted)));
            send(source, msg(source, "prefix.set-other", Map.of("target", target.name(), "formatted", formatted)));
        } else {
            send(source, msg(source, "prefix.set-self", Map.of("formatted", formatted)));
        }
        broadcastChange("prefix", source, target, formatted, "set");
        return 1;
    }

    private int handleSubtitle(ServerCommandSource source, String label, String[] args) {
        boolean isConsole = !(source.getEntity() instanceof ServerPlayerEntity);
        if (!isConsole && !(mod.hasPermission(source, "nickudon.subtitle") || mod.hasPermission(source, "nickudon.admin"))) {
            sendError(source, msg(source, "no-permission", null));
            return 0;
        }
        if (args.length < 2) {
            sendError(source, msg(source, "errors.usage-subtitle", Map.of("label", label, "sub", "subtitle")));
            return 0;
        }

        ServerPlayerEntity self = source.getEntity() instanceof ServerPlayerEntity player ? player : null;
        boolean canEditOthers = isConsole || mod.hasPermission(source, "nickudon.subtitle.others") || mod.hasPermission(source, "nickudon.admin");
        TargetRef target = null;
        int startIndex = 1;
        if (args.length >= 3 && canEditOthers) {
            TargetRef candidate = findTarget(source.getServer(), args[1]);
            if (candidate != null && (self == null || !candidate.uuid().equals(self.getUuid()))) {
                target = candidate;
                startIndex = 2;
            }
        }

        if (target == null) {
            if (self == null) {
                sendError(source, msg(source, "errors.specify-player", null));
                return 0;
            }
            target = new TargetRef(self.getUuid(), self.getName().getString(), self);
        }

        if (args.length <= startIndex) {
            sendError(source, msg(source, "errors.usage-subtitle", Map.of("label", label, "sub", "subtitle")));
            return 0;
        }

        String action = args[startIndex];
        if ("clear".equalsIgnoreCase(action)) {
            mod.names().clearSubtitle(target.uuid());
            if (target.online() != null) {
                mod.names().applyDisplay(target.online());
                mod.subtitles().remove(target.uuid());
            }
            if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
                send(target.online(), mod.messages().get(target.online(), "subtitle.cleared-by", Map.of("actor", source.getName())));
            }
            if (self != null && self.getUuid().equals(target.uuid())) {
                send(source, msg(source, "subtitle.cleared-self", null));
            } else {
                send(source, msg(source, "subtitle.cleared-other", Map.of("target", target.name())));
            }
            broadcastChange("subtitle", source, target, null, "cleared");
            return 1;
        }

        if ("on".equalsIgnoreCase(action) || "enable".equalsIgnoreCase(action)) {
            mod.names().setSubtitleEnabled(target.uuid(), true);
            if (target.online() != null) mod.subtitles().refresh(target.online());
            if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
                send(target.online(), mod.messages().get(target.online(), "subtitle.enabled-by", Map.of("actor", source.getName())));
            }
            if (self != null && self.getUuid().equals(target.uuid())) {
                send(source, msg(source, "subtitle.enabled-self", null));
            } else {
                send(source, msg(source, "subtitle.enabled-other", Map.of("target", target.name())));
            }
            broadcastChange("subtitle", source, target, null, "enabled");
            return 1;
        }

        if ("off".equalsIgnoreCase(action) || "disable".equalsIgnoreCase(action)) {
            mod.names().setSubtitleEnabled(target.uuid(), false);
            mod.subtitles().remove(target.uuid());
            if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
                send(target.online(), mod.messages().get(target.online(), "subtitle.disabled-by", Map.of("actor", source.getName())));
            }
            if (self != null && self.getUuid().equals(target.uuid())) {
                send(source, msg(source, "subtitle.disabled-self", null));
            } else {
                send(source, msg(source, "subtitle.disabled-other", Map.of("target", target.name())));
            }
            broadcastChange("subtitle", source, target, null, "disabled");
            return 1;
        }

        String subtitle = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        if (subtitle.length() > 64) subtitle = subtitle.substring(0, 64);
        subtitle = LegacyTextUtil.translateAlternateColorCodes('&', subtitle);
        if (!mod.names().hasColorCodes(subtitle)) {
            subtitle = mod.names().defaultSubtitleColorCode() + subtitle;
        }

        mod.names().setSubtitle(target.uuid(), subtitle);
        if (target.online() != null) {
            mod.names().applyDisplay(target.online());
            mod.subtitles().refresh(target.online());
        }

        String formatted = mod.names().formatSubtitle(
                target.name(),
                mod.names().getAlias(target.uuid()),
                mod.names().getPrefix(target.uuid()),
                subtitle,
                false);
        if (target.online() != null && (self == null || !target.online().getUuid().equals(self.getUuid()))) {
            send(target.online(), mod.messages().get(target.online(), "subtitle.set-by-other",
                    Map.of("actor", source.getName(), "formatted", formatted)));
            send(source, msg(source, "subtitle.set-other", Map.of("target", target.name(), "formatted", formatted)));
        } else {
            send(source, msg(source, "subtitle.set-self", Map.of("formatted", formatted)));
        }
        broadcastChange("subtitle", source, target, formatted, "set");
        return 1;
    }

    private void broadcastChange(String category,
                                 ServerCommandSource actor,
                                 TargetRef target,
                                 String formatted,
                                 String event) {
        String permission = mod.config().getString("broadcast." + category + ".permission",
                "nickudon.broadcast." + category);
        boolean selfChange = actor.getEntity() instanceof ServerPlayerEntity player && player.getUuid().equals(target.uuid());

        Map<String, String> vars = new HashMap<>();
        vars.put("actor", actor.getName());
        vars.put("target", target.name());
        if (formatted != null) vars.put("formatted", formatted);

        String key = "broadcast." + category + "." + event + "-" + (selfChange ? "self" : "other");
        for (ServerPlayerEntity viewer : actor.getServer().getPlayerManager().getPlayerList()) {
            boolean allowed = permission == null || permission.isBlank() || mod.hasPermission(viewer, permission);
            if (!allowed) continue;
            send(viewer, mod.messages().get(viewer, key, vars));
        }
        mod.console().sendInfo(actor.getServer(), mod.names().renderLegacy(actor.getServer(),
                mod.messages().get(mod.messages().defaultLocale(), key, vars)));
    }

    private TargetRef findTarget(MinecraftServer server, String name) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
        if (online != null) {
            return new TargetRef(online.getUuid(), online.getName().getString(), online);
        }

        return server.getApiServices().nameToIdCache()
                .findByName(name)
                .map(profile -> new TargetRef(profile.id(),
                        profile.name() == null || profile.name().isBlank()
                                ? profile.id().toString().substring(0, 8)
                                : profile.name(),
                        null))
                .orElse(null);
    }

    private String msg(ServerCommandSource source, String key, Map<String, String> vars) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return mod.messages().get(player, key, vars);
        }
        return mod.messages().get(mod.messages().defaultLocale(), key, vars);
    }

    private void send(ServerCommandSource source, String legacy) {
        if (mod.console().shouldBypassServerLogger() && mod.isLocalConsole(source)) {
            mod.console().sendInfo(mod.names().renderLegacy(source, legacy));
            return;
        }
        source.sendFeedback(() -> mod.names().renderLegacy(source, legacy), false);
    }

    private void sendError(ServerCommandSource source, String legacy) {
        if (mod.console().shouldBypassErrorLogger() && mod.isLocalConsole(source)) {
            mod.console().sendError(mod.names().renderLegacy(source, legacy));
            return;
        }
        source.sendError(mod.names().renderLegacy(source, legacy));
    }

    private void send(ServerPlayerEntity player, String legacy) {
        player.sendMessage(mod.names().renderLegacy(player, legacy), false);
    }

    private record TargetRef(UUID uuid, String name, ServerPlayerEntity online) {
    }

    private record CompletionContext(SuggestionsBuilder builder,
                                     List<String> tokens,
                                     int index) {
        static CompletionContext from(SuggestionsBuilder builder) {
            String remaining = builder.getRemaining();
            boolean trailingSpace = !remaining.isEmpty() && Character.isWhitespace(remaining.charAt(remaining.length() - 1));
            String trimmed = remaining.trim();

            List<String> tokens = trimmed.isEmpty()
                    ? List.of()
                    : Arrays.stream(trimmed.split("\\s+")).toList();

            int index;
            if (tokens.isEmpty()) {
                index = 0;
            } else if (trailingSpace) {
                index = tokens.size();
            } else {
                index = tokens.size() - 1;
            }

            return new CompletionContext(builder, tokens, index);
        }

        String token(int index) {
            return index >= 0 && index < tokens.size() ? tokens.get(index) : "";
        }

        CompletableFuture<Suggestions> suggest(List<String> values) {
            if (values.isEmpty()) {
                return Suggestions.empty();
            }

            LinkedHashSet<String> unique = new LinkedHashSet<>(values);
            int lastSpace = builder.getRemaining().lastIndexOf(' ');
            SuggestionsBuilder targetBuilder = lastSpace >= 0
                    ? builder.createOffset(builder.getStart() + lastSpace + 1)
                    : builder;
            return CommandSource.suggestMatching(unique, targetBuilder);
        }
    }
}
