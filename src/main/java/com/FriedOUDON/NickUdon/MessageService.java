package com.FriedOUDON.NickUdon;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MessageService {
    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> bundles = new HashMap<>();
    private final Map<UUID, String> overrides = new HashMap<>();
    private String defaultLocale;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultLocale = resolveDefaultLocale();
        load("en_US");
        load("ja_JP");
        if (!bundles.containsKey(defaultLocale)) {
            load(defaultLocale);
        }
    }

    public void reload() {
        bundles.clear();
        this.defaultLocale = resolveDefaultLocale();
        load("en_US");
        load("ja_JP");
        if (!bundles.containsKey(defaultLocale)) {
            load(defaultLocale);
        }
    }

    private void load(String code) {
        // Prefer external file in plugin data folder; fallback to bundled resource
        File external = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (external.exists()) {
            bundles.put(code, YamlConfiguration.loadConfiguration(external));
            return;
        }

        try (InputStreamReader r = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource("lang/" + code + ".yml")),
                StandardCharsets.UTF_8)) {
            bundles.put(code, YamlConfiguration.loadConfiguration(r));
        } catch (Exception ignored) {}
    }

    public void setPlayerLocaleOverride(UUID uuid, String code) {
        overrides.put(uuid, code);
    }

    private String localeOf(Player p) {
        String o = overrides.get(p.getUniqueId());
        if (o != null && bundles.containsKey(o)) return o;
        // Fallback to server default locale; Paper's Player#locale may be available in some environments.
        return bundles.containsKey(defaultLocale) ? defaultLocale : "en_US";
    }

    public String get(Player p, String key, Map<String,String> vars) {
        String resolved = resolve(localeOf(p), key, vars);
        return colorize(resolved, p);
    }

    public String get(String locale, String key, Map<String,String> vars) {
        String resolved = resolve(locale, key, vars);
        return colorize(resolved, null);
    }

    private String resolve(String locale, String key, Map<String,String> vars) {
        YamlConfiguration yml = bundles.getOrDefault(locale, bundles.get("en_US"));
        String s = yml != null ? yml.getString(key) : null;
        if (s == null && !"en_US".equalsIgnoreCase(locale)) {
            YamlConfiguration fallback = bundles.get("en_US");
            if (fallback != null) s = fallback.getString(key);
        }
        if (s == null) s = key;
        if (vars != null) {
            for (var e : vars.entrySet()) {
                s = s.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return s;
    }

    private String colorize(String text, Player viewer) {
        if (text == null || text.isEmpty()) return text;

        if (plugin instanceof HelloPlugin hello) {
            NameService names = hello.names();
            if (names != null) {
                boolean bedrockSafe = viewer != null && names.shouldDownsampleForViewer(viewer);
                text = names.applyHexColors(text, bedrockSafe);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    private String resolveDefaultLocale() {
        String cfg = plugin.getConfig().getString("defaultLocale", "");
        if (cfg != null && !cfg.isBlank()) {
            return cfg.replace('-', '_');
        }
        Locale jvm = Locale.getDefault();
        String code = jvm.toString();
        if (code == null || code.isBlank()) return "en_US";
        return code.replace('-', '_');
    }
}
