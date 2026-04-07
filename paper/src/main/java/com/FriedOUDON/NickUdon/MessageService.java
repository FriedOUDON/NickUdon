package com.FriedOUDON.NickUdon;

import com.FriedOUDON.NickUdon.common.LocalizedMessages;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MessageService {
    private final JavaPlugin plugin;
    private final LocalizedMessages messages = new LocalizedMessages();

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        messages.clearBundles();
        messages.setDefaultLocale(resolveDefaultLocale());
        load("en_US");
        load("ja_JP");
        if (messages.get(messages.defaultLocale(), "__probe__", null).equals("__probe__")) {
            load(messages.defaultLocale());
        }
    }

    public void setPlayerLocaleOverride(UUID uuid, String code) {
        messages.setPlayerLocaleOverride(uuid, code);
    }

    public String get(Player player, String key, Map<String, String> vars) {
        return colorize(messages.get(player.getUniqueId(), key, vars), player);
    }

    public String get(String locale, String key, Map<String, String> vars) {
        return colorize(messages.get(locale, key, vars), null);
    }

    public String defaultLocale() {
        return messages.defaultLocale();
    }

    private void load(String code) {
        File external = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (external.exists()) {
            messages.putBundle(code, PaperConfigAccess.readOnly(plugin, YamlConfiguration.loadConfiguration(external)));
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource("lang/" + code + ".yml")),
                StandardCharsets.UTF_8)) {
            messages.putBundle(code, PaperConfigAccess.readOnly(plugin, YamlConfiguration.loadConfiguration(reader)));
        } catch (Exception ignored) {
        }
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

    private String resolveDefaultLocale() {
        String cfg = plugin.getConfig().getString("defaultLocale", "");
        if (cfg != null && !cfg.isBlank()) return cfg.replace('-', '_');

        String jvmLocale = Locale.getDefault().toString();
        if (jvmLocale == null || jvmLocale.isBlank()) return "en_US";
        return jvmLocale.replace('-', '_');
    }
}
