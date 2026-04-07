package com.FriedOUDON.NickUdon;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelloTab implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        // /name is shorthand for /nickudon name
        if (cmd.getName().equalsIgnoreCase("name")) {
            return completeNameCommand(sender, args);
        }

        if (args.length == 1) {
            for (String s : new String[]{"reload","name","nick","alias","rename","prefix","subtitle","lang"}) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("lang")) {
                for (String s : new String[]{"en_US","ja_JP"}) if (s.startsWith(args[1])) out.add(s);
            } else if ("name nick alias rename".contains(args[0].toLowerCase())) {
                if (sender.hasPermission("nickudon.nickname.others")) {
                    String partial = args[1].toLowerCase();
                    Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName()); });
                }
                if ("clear".startsWith(args[1].toLowerCase())) out.add("clear");
            } else if (args[0].equalsIgnoreCase("prefix")) {
                if (sender.hasPermission("nickudon.prefix.others") || sender.hasPermission("nickudon.prefix") || sender.hasPermission("nickudon.admin")) {
                    String partial = args[1].toLowerCase();
                    Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName()); });
                }
                if ("clear".startsWith(args[1].toLowerCase())) out.add("clear");
            } else if (args[0].equalsIgnoreCase("subtitle")) {
                if (sender.hasPermission("nickudon.subtitle.others") || sender.hasPermission("nickudon.subtitle") || sender.hasPermission("nickudon.admin")) {
                    String partial = args[1].toLowerCase();
                    Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName()); });
                }
                if ("clear".startsWith(args[1].toLowerCase())) out.add("clear");
            }
        } else if (args.length == 3 && "name nick alias rename".contains(args[0].toLowerCase())) {
            if ("clear".startsWith(args[2].toLowerCase())) out.add("clear");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("prefix")) {
            if ("clear".startsWith(args[2].toLowerCase())) out.add("clear");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("subtitle")) {
            if ("clear".startsWith(args[2].toLowerCase())) out.add("clear");
        }
        return out;
    }

    private List<String> completeNameCommand(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("nickudon.nickname.others")) {
                String partial = args[0].toLowerCase();
                Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName()); });
            }
            if ("clear".startsWith(args[0].toLowerCase())) out.add("clear");
        } else if (args.length == 2) {
            if ("clear".startsWith(args[1].toLowerCase())) out.add("clear");
        }
        return out;
    }
}
