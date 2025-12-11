package com.FriedOUDON.NickUdon;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HelloPlugin extends JavaPlugin {
    private MessageService messages;
    private NameService names;
    private SubtitleService subtitles;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("lang/en_US.yml", false);
        saveResource("lang/ja_JP.yml", false);
        messages = new MessageService(this);
        names = new NameService(this);
        subtitles = new SubtitleService(this);
        setupEconomy();
        applyCommandAliases();

        HelloCommand helloCommand = new HelloCommand(this);
        HelloTab helloTab = new HelloTab();
        if (getCommand("nickudon") != null) {
            getCommand("nickudon").setExecutor(helloCommand);
            getCommand("nickudon").setTabCompleter(helloTab);
        }
        if (getCommand("name") != null) {
            getCommand("name").setExecutor(helloCommand);
            getCommand("name").setTabCompleter(helloTab);
        }

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DynmapSyncListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatFormatListener(this), this);
        Bukkit.getPluginManager().registerEvents(subtitles, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NickUdonExpansion(this).register();
            getLogger().info("Registered NickUdon PlaceholderAPI expansion");
        }

        getLogger().info("HelloPlugin enabled");
    }

    @Override
    public void onDisable() {
        if (subtitles != null) subtitles.shutdown();
        getLogger().info("HelloPlugin disabled");
    }

    public MessageService messages() { return messages; }
    public NameService names() { return names; }
    public SubtitleService subtitles() { return subtitles; }
    public Economy economy() { return economy; }

    public void applyCommandAliases() {
        PluginCommand cmd = getCommand("nickudon");
        if (cmd == null) {
            getLogger().warning("Could not find /nickudon command to apply aliases");
            return;
        }

        List<String> configured = getConfig().getStringList("commandAliases");
        Set<String> cleaned = new LinkedHashSet<>();
        for (String alias : configured) {
            if (alias == null) continue;
            String normalized = alias.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;
            if (normalized.equals("nickudon")) continue;
            cleaned.add(normalized);
        }
        cmd.setAliases(new ArrayList<>(cleaned));

        CommandMap commandMap = findCommandMap();
        if (commandMap == null) {
            getLogger().warning("Could not access command map to register /nickudon aliases");
            return;
        }
        cmd.unregister(commandMap);
        Map<String, org.bukkit.command.Command> known = commandMap.getKnownCommands();
        List<String> staleKeys = new ArrayList<>();
        for (Map.Entry<String, org.bukkit.command.Command> entry : known.entrySet()) {
            if (entry.getValue() == cmd) staleKeys.add(entry.getKey());
        }
        for (String key : staleKeys) {
            try { known.remove(key); }
            catch (UnsupportedOperationException ex) { break; }
        }
        cmd.setLabel(cmd.getName());

        String label = cmd.getName().toLowerCase(Locale.ROOT);
        String fallback = getDescription().getName().toLowerCase(Locale.ROOT);
        boolean registered = commandMap.register(label, fallback, cmd);
        if (!registered) {
            getLogger().warning("Failed to register /nickudon command aliases (conflict?)");
        }
    }

    private CommandMap findCommandMap() {
        try {
            Method getter = getServer().getClass().getMethod("getCommandMap");
            Object map = getter.invoke(getServer());
            if (map instanceof CommandMap commandMap) return commandMap;
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    private void setupEconomy() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException e) {
            getLogger().info("Vault not found; economy payments disabled");
            return;
        }
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            if (economy != null) {
                getLogger().info("Hooked into Vault economy: " + economy.getName());
            }
        }
        if (economy == null) {
            getLogger().info("No Vault economy provider found; payments disabled");
        }
    }
}
