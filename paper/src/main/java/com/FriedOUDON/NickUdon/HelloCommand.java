package com.FriedOUDON.NickUdon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class HelloCommand implements CommandExecutor {
    private final HelloPlugin plugin;

    public HelloCommand(HelloPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /name is shorthand for /nickudon name
        if (command.getName().equalsIgnoreCase("name")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "name";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
            label = "nickudon"; // avoid usage confusion
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            // reload
            if (sub.equals("reload")) {
                if (!sender.hasPermission("nickudon.admin")) {
                    sender.sendMessage(ChatColor.RED + msg(sender, "no-permission", null));
                    return true;
                }
                plugin.reloadConfig();
                plugin.applyCommandAliases();
                plugin.messages().reload();
                plugin.names().reload();
                if (plugin.subtitles() != null) plugin.subtitles().reload();
                sender.sendMessage(ChatColor.GREEN + msg(sender, "reloaded", null));
                return true;
            }

            // lang <code>
            if (sub.equals("lang")) {
                if (!(sender instanceof Player)) { sender.sendMessage(msg(sender, "errors.players-only", null)); return true; }
                Player p = (Player) sender;
                if (args.length < 2) {
                    Map<String,String> vars = Map.of("label", label);
                    sender.sendMessage(msg(sender, "errors.usage-lang", vars));
                    return true;
                }
                String code = args[1];
                plugin.messages().setPlayerLocaleOverride(p.getUniqueId(), code);
                Map<String,String> vars = new HashMap<>();
                vars.put("locale", code);
                p.sendMessage(ChatColor.AQUA + plugin.messages().get(p, "current-locale", vars));
                return true;
            }

            // name/nick/alias/rename
            Set<String> nameSubs = Set.of("name","nick","alias","rename");
            if (nameSubs.contains(sub)) {
                if (!sender.hasPermission("nickudon.nickname")) { sender.sendMessage(ChatColor.RED + msg(sender, "no-permission", null)); return true; }
                if (args.length < 2) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-name", vars));
                    return true;
                }

                boolean canEditOthers = sender.hasPermission("nickudon.nickname.others") || sender.hasPermission("nickudon.admin");
                OfflinePlayer target = null;
                int aliasStartIndex = 1;

                if (args.length >= 3 && canEditOthers) {
                    OfflinePlayer candidate = findTargetPlayer(args[1]);
                    Player self = (sender instanceof Player) ? (Player) sender : null;
                    if (candidate != null && (self == null || !candidate.getUniqueId().equals(self.getUniqueId()))) {
                        target = candidate;
                        aliasStartIndex = 2;
                    }
                }

                if (target == null) {
                    if (!(sender instanceof Player)) { sender.sendMessage(msg(sender, "errors.specify-player", null)); return true; }
                    target = (Player) sender;
                }

                String targetName = target.getName();
                if (targetName == null || targetName.isBlank()) targetName = target.getUniqueId().toString().substring(0, 8);

                if (args.length <= aliasStartIndex) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-name", vars));
                    return true;
                }
                String arg1 = args[aliasStartIndex];
                if (arg1.equalsIgnoreCase("clear")) {
                    if (!chargeIfRequired(sender, "alias.clear", target)) return true;
                    plugin.names().clearAlias(target.getUniqueId());
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.names().applyDisplay(online);
                        plugin.names().clearTeamEntry(online);
                    }
                if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                    Map<String,String> clearedByVars = Map.of("actor", sender.getName());
                    online.sendMessage(msg(online, "nickname.cleared-by", clearedByVars));
                }
                if (target == sender) {
                    sender.sendMessage(msg(sender, "nickname.cleared-self", null));
                } else {
                    Map<String,String> clearedForVars = Map.of("target", targetName);
                    sender.sendMessage(msg(sender, "nickname.cleared-other", clearedForVars));
                }
                broadcastChange("alias", sender, target, targetName, null, "cleared");
                return true;
            }

            String alias = String.join(" ", Arrays.copyOfRange(args, aliasStartIndex, args.length));
            if (alias.length() > 32) alias = alias.substring(0, 32);

                // & color code support
                alias = ChatColor.translateAlternateColorCodes('&', alias);

                // apply default color when none specified
                String defaultColorName = plugin.getConfig().getString("defaultAliasColor", "AQUA");
                ChatColor defaultColor;
                try { defaultColor = ChatColor.valueOf(defaultColorName.toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException e) { defaultColor = ChatColor.AQUA; }
                if (!plugin.names().hasColorCodes(alias)) alias = defaultColor + alias;

                // conflict check
                UUID conflict = plugin.names().findAliasConflict(alias, target.getUniqueId());
                if (conflict != null) {
                    String other = Bukkit.getOfflinePlayer(conflict).getName();
                    if (other == null) other = conflict.toString().substring(0, 8);
                    Map<String,String> conflictVars = Map.of("other", other);
                    sender.sendMessage(ChatColor.RED + msg(sender, "nickname.conflict", conflictVars));
                    return true;
                }

                if (!chargeIfRequired(sender, "alias.set", target)) return true;

                plugin.names().setAlias(target.getUniqueId(), alias);
                Player online = target.getPlayer();
                if (online != null) plugin.names().applyDisplay(online);

                String formatted = plugin.names().format(online, targetName, alias, plugin.names().getPrefix(target.getUniqueId()));
                if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                    Map<String,String> setByVars = Map.of("actor", sender.getName(), "formatted", formatted);
                    online.sendMessage(msg(online, "nickname.set-by-other", setByVars));

                    Map<String,String> setForVars = Map.of("target", targetName, "formatted", formatted);
                    sender.sendMessage(msg(sender, "nickname.set-other", setForVars));
                } else {
                    Map<String,String> setVars = Map.of("formatted", formatted);
                    sender.sendMessage(msg(sender, "nickname.set-self", setVars));
                }
                broadcastChange("alias", sender, target, targetName, formatted, "set");
                return true;
            }

            // prefix (admin/console only)
            if (sub.equals("prefix")) {
                boolean isConsole = !(sender instanceof Player);
                if (!isConsole && !(sender.hasPermission("nickudon.prefix") || sender.hasPermission("nickudon.admin"))) {
                    sender.sendMessage(ChatColor.RED + msg(sender, "no-permission", null));
                    return true;
                }

                if (args.length < 2) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-prefix", vars));
                    return true;
                }

                boolean canEditOthers = isConsole
                        || sender.hasPermission("nickudon.prefix.others")
                        || sender.hasPermission("nickudon.admin");
                OfflinePlayer target = null;
                int startIndex = 1;

                if (args.length >= 3 && canEditOthers) {
                    OfflinePlayer candidate = findTargetPlayer(args[1]);
                    Player self = (sender instanceof Player) ? (Player) sender : null;
                    if (candidate != null && (self == null || !candidate.getUniqueId().equals(self.getUniqueId()))) {
                        target = candidate;
                        startIndex = 2;
                    }
                }

                if (target == null) {
                    if (!(sender instanceof Player)) { sender.sendMessage(msg(sender, "errors.specify-player", null)); return true; }
                    target = (Player) sender;
                }

                String targetName = target.getName();
                if (targetName == null || targetName.isBlank()) targetName = target.getUniqueId().toString().substring(0, 8);

                if (args.length <= startIndex) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-prefix", vars));
                    return true;
                }

                String arg1 = args[startIndex];
                if (arg1.equalsIgnoreCase("clear")) {
                    if (!chargeIfRequired(sender, "prefix.clear", target)) return true;
                    plugin.names().clearPrefix(target.getUniqueId());
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.names().applyDisplay(online);
                        plugin.names().clearTeamEntry(online);
                    }
                    if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                        Map<String,String> clearedByVars = Map.of("actor", sender.getName());
                        online.sendMessage(msg(online, "prefix.cleared-by", clearedByVars));
                    }
                    if (target == sender) {
                        sender.sendMessage(msg(sender, "prefix.cleared-self", null));
                    } else {
                        Map<String,String> clearedForVars = Map.of("target", targetName);
                        sender.sendMessage(msg(sender, "prefix.cleared-other", clearedForVars));
                    }
                    broadcastChange("prefix", sender, target, targetName, null, "cleared");
                    return true;
                }

                String prefix = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
                if (prefix.length() > 32) prefix = prefix.substring(0, 32);
                prefix = ChatColor.translateAlternateColorCodes('&', prefix);
                if (!plugin.names().hasColorCodes(prefix)) {
                    String defaultColorName = plugin.getConfig().getString("defaultPrefixColor",
                            plugin.getConfig().getString("defaultAliasColor", "AQUA"));
                    ChatColor defaultColor;
                    try { defaultColor = ChatColor.valueOf(defaultColorName.toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException e) { defaultColor = ChatColor.AQUA; }
                    prefix = defaultColor + prefix;
                }

                if (!chargeIfRequired(sender, "prefix.set", target)) return true;

                plugin.names().setPrefix(target.getUniqueId(), prefix);
                Player online = target.getPlayer();
                if (online != null) plugin.names().applyDisplay(online);

                String formatted = plugin.names().format(online, targetName, plugin.names().getAlias(target.getUniqueId()), prefix);
                if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                    Map<String,String> setByVars = Map.of("actor", sender.getName(), "formatted", formatted);
                    online.sendMessage(msg(online, "prefix.set-by-other", setByVars));

                    Map<String,String> setForVars = Map.of("target", targetName, "formatted", formatted);
                    sender.sendMessage(msg(sender, "prefix.set-other", setForVars));
                } else {
                    Map<String,String> setVars = Map.of("formatted", formatted);
                    sender.sendMessage(msg(sender, "prefix.set-self", setVars));
                }
                broadcastChange("prefix", sender, target, targetName, formatted, "set");
                return true;
            }

            // subtitle
            if (sub.equals("subtitle")) {
                boolean isConsole = !(sender instanceof Player);
                if (!isConsole && !(sender.hasPermission("nickudon.subtitle") || sender.hasPermission("nickudon.admin"))) {
                    sender.sendMessage(ChatColor.RED + msg(sender, "no-permission", null));
                    return true;
                }

                if (args.length < 2) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-subtitle", vars));
                    return true;
                }

                boolean canEditOthers = isConsole
                        || sender.hasPermission("nickudon.subtitle.others")
                        || sender.hasPermission("nickudon.admin");
                OfflinePlayer target = null;
                int startIndex = 1;

                if (args.length >= 3 && canEditOthers) {
                    OfflinePlayer candidate = findTargetPlayer(args[1]);
                    Player self = (sender instanceof Player) ? (Player) sender : null;
                    if (candidate != null && (self == null || !candidate.getUniqueId().equals(self.getUniqueId()))) {
                        target = candidate;
                        startIndex = 2;
                    }
                }

                if (target == null) {
                    if (!(sender instanceof Player)) { sender.sendMessage(msg(sender, "errors.specify-player", null)); return true; }
                    target = (Player) sender;
                }

                String targetName = target.getName();
                if (targetName == null || targetName.isBlank()) targetName = target.getUniqueId().toString().substring(0, 8);

                if (args.length <= startIndex) {
                    Map<String,String> vars = Map.of("label", label, "sub", sub);
                    sender.sendMessage(msg(sender, "errors.usage-subtitle", vars));
                    return true;
                }

                String arg1 = args[startIndex];
                if (arg1.equalsIgnoreCase("clear")) {
                    if (!chargeIfRequired(sender, "subtitle.clear", target)) return true;
                    plugin.names().clearSubtitle(target.getUniqueId());
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.names().applyDisplay(online);
                        plugin.subtitles().remove(online.getUniqueId());
                    }
                    if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                        Map<String,String> clearedByVars = Map.of("actor", sender.getName());
                        online.sendMessage(msg(online, "subtitle.cleared-by", clearedByVars));
                    }
                    if (target == sender) {
                        sender.sendMessage(msg(sender, "subtitle.cleared-self", null));
                    } else {
                        Map<String,String> clearedForVars = Map.of("target", targetName);
                        sender.sendMessage(msg(sender, "subtitle.cleared-other", clearedForVars));
                    }
                    broadcastChange("subtitle", sender, target, targetName, null, "cleared");
                    return true;
                } else if (arg1.equalsIgnoreCase("on") || arg1.equalsIgnoreCase("enable")) {
                    if (!chargeIfRequired(sender, "subtitle.enable", target)) return true;
                    plugin.names().setSubtitleEnabled(target.getUniqueId(), true);
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.subtitles().refresh(online);
                    }
                    if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                        Map<String,String> enabledByVars = Map.of("actor", sender.getName());
                        online.sendMessage(msg(online, "subtitle.enabled-by", enabledByVars));
                    }
                    if (target == sender) {
                        sender.sendMessage(msg(sender, "subtitle.enabled-self", null));
                    } else {
                        Map<String,String> enabledForVars = Map.of("target", targetName);
                        sender.sendMessage(msg(sender, "subtitle.enabled-other", enabledForVars));
                    }
                    broadcastChange("subtitle", sender, target, targetName, null, "enabled");
                    return true;
                } else if (arg1.equalsIgnoreCase("off") || arg1.equalsIgnoreCase("disable")) {
                    if (!chargeIfRequired(sender, "subtitle.disable", target)) return true;
                    plugin.names().setSubtitleEnabled(target.getUniqueId(), false);
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.subtitles().remove(online.getUniqueId());
                    }
                    if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                        Map<String,String> disabledByVars = Map.of("actor", sender.getName());
                        online.sendMessage(msg(online, "subtitle.disabled-by", disabledByVars));
                    }
                    if (target == sender) {
                        sender.sendMessage(msg(sender, "subtitle.disabled-self", null));
                    } else {
                        Map<String,String> disabledForVars = Map.of("target", targetName);
                        sender.sendMessage(msg(sender, "subtitle.disabled-other", disabledForVars));
                    }
                    broadcastChange("subtitle", sender, target, targetName, null, "disabled");
                    return true;
                }

                String subtitle = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
                if (subtitle.length() > 64) subtitle = subtitle.substring(0, 64);
                subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
                if (!plugin.names().hasColorCodes(subtitle)) {
                    subtitle = plugin.names().getDefaultSubtitleColor() + subtitle;
                }

                if (!chargeIfRequired(sender, "subtitle.set", target)) return true;

                plugin.names().setSubtitle(target.getUniqueId(), subtitle);
                Player online = target.getPlayer();
                if (online != null) {
                    plugin.names().applyDisplay(online);
                    plugin.subtitles().refresh(online);
                }

                String formatted = plugin.names().formatSubtitle(
                        online,
                        targetName,
                        plugin.names().getAlias(target.getUniqueId()),
                        plugin.names().getPrefix(target.getUniqueId()),
                        subtitle);
                if (online != null && !(sender instanceof Player && ((Player) sender).getUniqueId().equals(online.getUniqueId()))) {
                    Map<String,String> setByVars = Map.of("actor", sender.getName(), "formatted", formatted);
                    online.sendMessage(msg(online, "subtitle.set-by-other", setByVars));

                    Map<String,String> setForVars = Map.of("target", targetName, "formatted", formatted);
                    sender.sendMessage(msg(sender, "subtitle.set-other", setForVars));
                } else {
                    Map<String,String> setVars = Map.of("formatted", formatted);
                    sender.sendMessage(msg(sender, "subtitle.set-self", setVars));
                }
                broadcastChange("subtitle", sender, target, targetName, formatted, "set");
                return true;
            }

            sender.sendMessage(msg(sender, "unknown-subcommand", null));
            return true;
        }

        // /nickudon
        if (sender instanceof Player) {
            Player p = (Player) sender;
            Map<String,String> vars = new HashMap<>();
            vars.put("player", p.getName());
            p.sendMessage(ChatColor.GREEN + plugin.messages().get(p, "hello.self", vars));
        } else {
            sender.sendMessage(msg(sender, "hello.console", null));
        }
        return true;
    }

    private OfflinePlayer findTargetPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        try {
            OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
            if (cached != null) return cached;
        } catch (NoSuchMethodError ignored) {}

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) return offline;
        return null;
    }

    private String msg(CommandSender sender, String key, Map<String,String> vars) {
        if (sender instanceof Player p) return plugin.messages().get(p, key, vars);
        return plugin.messages().get(plugin.messages().defaultLocale(), key, vars);
    }

    private void broadcastChange(String category,
                                 CommandSender actor,
                                 OfflinePlayer target,
                                 String targetName,
                                 String formatted,
                                 String event) {
        String perm = plugin.getConfig().getString("broadcast." + category + ".permission",
                "nickudon.broadcast." + category);
        UUID targetId = target.getUniqueId();
        boolean self = actor instanceof Player && ((Player) actor).getUniqueId().equals(targetId);
        String effectiveName = (targetName == null || targetName.isBlank())
                ? targetId.toString().substring(0, 8)
                : targetName;

        Map<String,String> vars = new HashMap<>();
        vars.put("actor", actor.getName());
        vars.put("target", effectiveName);
        if (formatted != null) vars.put("formatted", formatted);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean allowed = viewer.hasPermission("nickudon.admin");
            if (!allowed) {
                if (perm == null || perm.isBlank()) {
                    allowed = true; // blank permission means broadcast to everyone
                } else {
                    allowed = viewer.hasPermission(perm);
                }
            }
            if (!allowed) continue;

            String key = "broadcast." + category + "." + event + "-" + (self ? "self" : "other");
            viewer.sendMessage(msg(viewer, key, vars));
        }

        var console = plugin.getServer().getConsoleSender();
        if (console != null) {
            String key = "broadcast." + category + "." + event + "-" + (self ? "self" : "other");
            console.sendMessage(msg(console, key, vars));
        }
    }

    private boolean chargeIfRequired(CommandSender actor, String actionPath, OfflinePlayer target) {
        if (!(actor instanceof Player p)) return true;
        if (!plugin.getConfig().getBoolean("payments.enabled", false)) return true;

        String category = actionPath.contains(".")
                ? actionPath.substring(0, actionPath.indexOf('.'))
                : actionPath;

        if (p.hasPermission("nickudon.payments.bypass.*")) return true;
        String bypassPermission = switch (category) {
            case "alias" -> "nickudon.payments.bypass.alias";
            case "prefix" -> "nickudon.payments.bypass.prefix";
            case "subtitle" -> "nickudon.payments.bypass.subtitle";
            default -> null;
        };
        if (bypassPermission != null && p.hasPermission(bypassPermission)) return true;

        boolean editingOthers = target != null && !target.getUniqueId().equals(p.getUniqueId());
        if (editingOthers) {
            String othersBypass = "nickudon.payments.bypass." + category + ".others";
            if (p.hasPermission(othersBypass)) return true;
            String othersPermission = switch (category) {
                case "alias" -> "nickudon.nickname.others";
                case "prefix" -> "nickudon.prefix.others";
                case "subtitle" -> "nickudon.subtitle.others";
                default -> null;
            };
            if (othersPermission != null && (p.hasPermission(othersPermission) || p.hasPermission("nickudon.admin"))) {
                return true;
            }
        }

        double cost = plugin.getConfig().getDouble("payments." + actionPath, 0.0);
        if (cost <= 0.0) return true;
        if (plugin.economy() == null) {
            actor.sendMessage(ChatColor.RED + msg(actor, "payments.no-economy", null));
            return false;
        }
        if (!plugin.economy().has(p, cost)) {
            Map<String,String> vars = Map.of("cost", plugin.economy().format(cost));
            actor.sendMessage(ChatColor.RED + msg(actor, "payments.insufficient", vars));
            return false;
        }
        plugin.economy().withdrawPlayer(p, cost);
        Map<String,String> vars = Map.of("cost", plugin.economy().format(cost));
        actor.sendMessage(ChatColor.GREEN + msg(actor, "payments.charged", vars));
        return true;
    }
}
