package com.FriedOUDON.NickUdon.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LocalizedMessages {
    private final Map<String, ConfigAccess> bundles = new HashMap<>();
    private final Map<UUID, String> overrides = new HashMap<>();
    private String defaultLocale = "en_US";

    public void clearBundles() {
        bundles.clear();
    }

    public void putBundle(String locale, ConfigAccess bundle) {
        if (locale == null || locale.isBlank() || bundle == null) return;
        bundles.put(locale, bundle);
    }

    public void setDefaultLocale(String defaultLocale) {
        if (defaultLocale == null || defaultLocale.isBlank()) {
            this.defaultLocale = "en_US";
            return;
        }
        this.defaultLocale = defaultLocale.replace('-', '_');
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public void setPlayerLocaleOverride(UUID uuid, String locale) {
        if (uuid == null || locale == null || locale.isBlank()) return;
        overrides.put(uuid, locale);
    }

    public String get(UUID playerId, String key, Map<String, String> vars) {
        return resolve(localeOf(playerId), key, vars);
    }

    public String get(String locale, String key, Map<String, String> vars) {
        return resolve(locale, key, vars);
    }

    private String localeOf(UUID playerId) {
        if (playerId != null) {
            String override = overrides.get(playerId);
            if (override != null && bundles.containsKey(override)) return override;
        }
        return bundles.containsKey(defaultLocale) ? defaultLocale : "en_US";
    }

    private String resolve(String locale, String key, Map<String, String> vars) {
        ConfigAccess bundle = bundles.getOrDefault(locale, bundles.get("en_US"));
        String value = bundle != null ? bundle.getString(key) : null;

        if (value == null && !"en_US".equalsIgnoreCase(locale)) {
            ConfigAccess fallback = bundles.get("en_US");
            if (fallback != null) value = fallback.getString(key);
        }

        if (value == null) value = key;

        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                value = value.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return value;
    }
}
