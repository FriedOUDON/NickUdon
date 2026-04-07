package com.FriedOUDON.NickUdon.common;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NickProfileRepository {
    private static final String DATA_FILE_NAME = "data.yml";
    private static final String ALIASES_ROOT = "aliases";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final MutableConfigAccess data;
    private final ConfigAccess config;
    private final NickFormatter formatter;

    public NickProfileRepository(MutableConfigAccess data, ConfigAccess config, NickFormatter formatter) {
        this.data = data;
        this.config = config;
        this.formatter = formatter;
        migrateLegacyAliases();
    }

    public String dataFileName() {
        return DATA_FILE_NAME;
    }

    public void reload() {
        data.load();
        migrateLegacyAliases();
    }

    public void setAlias(UUID uuid, String alias) {
        if (alias == null || alias.isBlank()) {
            data.set(ALIASES_ROOT + "." + uuid, null);
        } else {
            data.set(ALIASES_ROOT + "." + uuid, toStorageFormat(alias));
        }
        data.save();
    }

    public void clearAlias(UUID uuid) {
        data.set(ALIASES_ROOT + "." + uuid, null);
        data.save();
    }

    public String getAlias(UUID uuid) {
        return data.getString(ALIASES_ROOT + "." + uuid);
    }

    public void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            data.set("prefix." + uuid, null);
        } else {
            data.set("prefix." + uuid, toStorageFormat(prefix));
        }
        data.save();
    }

    public void clearPrefix(UUID uuid) {
        data.set("prefix." + uuid, null);
        data.save();
    }

    public String getPrefix(UUID uuid) {
        return data.getString("prefix." + uuid);
    }

    public void setSubtitle(UUID uuid, String subtitle) {
        if (subtitle == null || subtitle.isBlank()) {
            data.set("subtitle." + uuid, null);
        } else {
            data.set("subtitle." + uuid, toStorageFormat(subtitle));
        }
        data.save();
    }

    public void clearSubtitle(UUID uuid) {
        data.set("subtitle." + uuid, null);
        data.save();
    }

    public String getSubtitle(UUID uuid) {
        return data.getString("subtitle." + uuid);
    }

    public void setSubtitleEnabled(UUID uuid, boolean enabled) {
        data.set("subtitleEnabled." + uuid, enabled);
        data.save();
    }

    public boolean isSubtitleEnabled(UUID uuid) {
        return data.getBoolean("subtitleEnabled." + uuid, true);
    }

    public UUID findAliasConflict(String candidateRaw, UUID self, Iterable<PlayerIdentity> onlinePlayers) {
        String candidate = formatter.normalizeAlias(candidateRaw);
        if (candidate.isBlank()) return null;
        if (!config.getBoolean("aliasUnique.enabled", true)) return null;

        for (String key : data.getKeys(ALIASES_ROOT)) {
            String raw = data.getString(ALIASES_ROOT + "." + key);
            if (raw == null) continue;
            if (self != null && key.equals(self.toString())) continue;
            if (!formatter.normalizeAlias(raw).equals(candidate)) continue;

            try {
                return UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        if (!config.getBoolean("aliasUnique.denySameAsOriginal", true)) return null;

        for (PlayerIdentity player : onlinePlayers) {
            if (self != null && player.uuid().equals(self)) continue;
            if (formatter.normalizeAlias(player.name()).equals(candidate)) {
                return player.uuid();
            }
        }

        return null;
    }

    private void migrateLegacyAliases() {
        boolean migrated = false;

        for (String key : new HashSet<>(data.getKeys(""))) {
            if (ALIASES_ROOT.equals(key)) continue;
            if (!UUID_PATTERN.matcher(key).matches()) continue;

            String value = data.getString(key);
            data.set(key, null);
            if (value != null && data.getString(ALIASES_ROOT + "." + key) == null) {
                data.set(ALIASES_ROOT + "." + key, toStorageFormat(value));
            }
            migrated = true;
        }

        if (sanitizeSection(ALIASES_ROOT)) migrated = true;
        if (sanitizeSection("prefix")) migrated = true;
        if (sanitizeSection("subtitle")) migrated = true;
        if (migrated) data.save();
    }

    private boolean sanitizeSection(String path) {
        Set<String> keys = data.getKeys(path);
        if (keys.isEmpty()) return false;

        boolean changed = false;
        for (String key : keys) {
            String value = data.getString(path + "." + key);
            if (value == null) continue;

            String clean = toStorageFormat(value);
            if (!clean.equals(value)) {
                data.set(path + "." + key, clean);
                changed = true;
            }
        }
        return changed;
    }

    private String toStorageFormat(String text) {
        return text == null ? null : text.replace('\u00A7', '&');
    }
}
