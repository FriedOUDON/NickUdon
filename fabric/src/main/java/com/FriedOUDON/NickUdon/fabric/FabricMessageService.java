package com.FriedOUDON.NickUdon.fabric;

import com.FriedOUDON.NickUdon.common.LocalizedMessages;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class FabricMessageService {
    private final NickUdonFabric mod;
    private final LocalizedMessages messages = new LocalizedMessages();

    FabricMessageService(NickUdonFabric mod) {
        this.mod = mod;
        reload();
    }

    void reload() {
        messages.clearBundles();
        messages.setDefaultLocale(resolveDefaultLocale());
        load("en_US");
        load("ja_JP");
        load(messages.defaultLocale());
    }

    void setPlayerLocaleOverride(UUID uuid, String code) {
        messages.setPlayerLocaleOverride(uuid, code);
    }

    String get(ServerPlayerEntity player, String key, Map<String, String> vars) {
        return colorize(messages.get(player.getUuid(), key, vars), player);
    }

    String get(String locale, String key, Map<String, String> vars) {
        return colorize(messages.get(locale, key, vars), null);
    }

    Text asText(ServerPlayerEntity player, String key, Map<String, String> vars) {
        return mod.names().renderLegacy(player, get(player, key, vars));
    }

    Text asText(String locale, String key, Map<String, String> vars) {
        return mod.names().renderLegacy(mod.server(), get(locale, key, vars));
    }

    String defaultLocale() {
        return messages.defaultLocale();
    }

    private void load(String code) {
        if (code == null || code.isBlank()) return;

        Path external = mod.baseDir().resolve("lang").resolve(code + ".yml");
        if (Files.exists(external)) {
            try {
                FabricYamlConfigAccess access = FabricYamlConfigAccess.fromFile(external, mod::warn);
                mergeBundledDefaults(code, access);
                messages.putBundle(code, access);
            } catch (Exception e) {
                mod.warn("Failed to read lang/" + code + ".yml: " + e.getMessage());
            }
            return;
        }

        try (Reader reader = mod.openBundledText("lang/" + code + ".yml")) {
            if (reader != null) {
                messages.putBundle(code, FabricYamlConfigAccess.fromReader(reader, mod::warn));
            }
        } catch (Exception e) {
            mod.warn("Failed to load bundled lang/" + code + ".yml: " + e.getMessage());
        }
    }

    private void mergeBundledDefaults(String code, FabricYamlConfigAccess external) {
        try (Reader reader = mod.openBundledText("lang/" + code + ".yml")) {
            if (reader == null) {
                return;
            }

            FabricYamlConfigAccess defaults = FabricYamlConfigAccess.fromReader(reader, mod::warn);
            if (external.mergeMissing(defaults)) {
                external.save();
            }
        } catch (Exception e) {
            mod.warn("Failed to merge bundled lang/" + code + ".yml: " + e.getMessage());
        }
    }

    private String colorize(String text, ServerPlayerEntity viewer) {
        if (text == null || text.isEmpty()) return text;

        boolean bedrockSafe = viewer != null && mod.names().shouldDownsampleForViewer(viewer);
        return mod.names().colorize(text, bedrockSafe);
    }

    private String resolveDefaultLocale() {
        String configured = mod.config().getString("defaultLocale", "");
        if (configured != null && !configured.isBlank()) return configured.replace('-', '_');

        String locale = Locale.getDefault().toString();
        if (locale == null || locale.isBlank()) return "en_US";
        return locale.replace('-', '_');
    }
}
