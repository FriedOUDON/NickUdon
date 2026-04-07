package com.FriedOUDON.NickUdon.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NickFormatter {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[A-Fa-f0-9]{6}");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("\u00A7x(\u00A7[0-9A-Fa-f]){6}");

    private final ConfigAccess config;

    public NickFormatter(ConfigAccess config) {
        this.config = config;
    }

    public String normalizeAlias(String alias) {
        if (alias == null) return "";

        String normalized = convertHexToLegacy(alias);
        normalized = LegacyTextUtil.stripColor(normalized);
        if (normalized == null) normalized = alias;
        normalized = normalized.trim();

        if (config.getBoolean("aliasUnique.normalizeNFKC", true)) {
            normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        }
        if (config.getBoolean("aliasUnique.caseInsensitive", true)) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    public String format(String original,
                         String alias,
                         String prefix,
                         boolean bedrockSafe,
                         UnaryOperator<String> placeholderResolver) {
        String fmt = config.getString("nameFormat", "%prefix%%original%[%alias%]");
        String fmtNoAlias = config.getString("nameFormatNoAlias", "%prefix%%original%");
        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);

        String cleanAlias = alias != null
                ? applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', alias), bedrockSafe)
                : "";
        String aliasColors = LegacyTextUtil.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) {
            aliasColors = defaultAliasColorCode();
        }
        if (aliasColors == null) aliasColors = "";

        if (cleanAlias.isBlank()) {
            return resolveExternal(fmtNoAlias.replace("%original%", original)
                    .replace("%alias%", "")
                    .replace("%aliasC%", "")
                    .replace("%prefix%", prefixParts.formatted())
                    .replace("%prefixC%", prefixParts.colors())
                    .replace("%reset%", LegacyTextUtil.RESET), placeholderResolver);
        }

        return resolveExternal(fmt.replace("%original%", original)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%prefix%", prefixParts.formatted())
                .replace("%prefixC%", prefixParts.colors())
                .replace("%reset%", LegacyTextUtil.RESET), placeholderResolver);
    }

    public String formatChatName(String original,
                                 String alias,
                                 String prefix,
                                 boolean bedrockSafe,
                                 UnaryOperator<String> placeholderResolver) {
        String fmtAlias = config.getString("chatNameFormat", "%prefix%%aliasC%%alias%%reset%(%original%)");
        String fmtNoAlias = config.getString("chatNameFormatNoAlias", "%prefix%(%original%)");

        if (alias == null || alias.isBlank() || LegacyTextUtil.stripColor(alias).isBlank()) {
            return resolveExternal(fmtNoAlias.replace("%original%", original)
                    .replace("%prefix%", formatPrefix(prefix, bedrockSafe).formatted())
                    .replace("%alias%", "")
                    .replace("%aliasC%", "")
                    .replace("%reset%", LegacyTextUtil.RESET), placeholderResolver);
        }

        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);
        String cleanAlias = applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', alias), bedrockSafe);
        String aliasColors = LegacyTextUtil.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) {
            aliasColors = defaultAliasColorCode();
        }
        if (aliasColors == null) aliasColors = "";

        return resolveExternal(fmtAlias.replace("%original%", original)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%prefix%", prefixParts.formatted())
                .replace("%prefixC%", prefixParts.colors())
                .replace("%reset%", LegacyTextUtil.RESET), placeholderResolver);
    }

    public String formatSubtitle(String original,
                                 String alias,
                                 String prefix,
                                 String subtitleRaw,
                                 boolean bedrockSafe,
                                 UnaryOperator<String> placeholderResolver) {
        if (subtitleRaw == null || subtitleRaw.isBlank()) return "";

        String cleanSubtitle = applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', subtitleRaw), bedrockSafe);
        if (Objects.equals(LegacyTextUtil.stripColor(cleanSubtitle), cleanSubtitle)) {
            cleanSubtitle = defaultSubtitleColorCode() + cleanSubtitle;
        }

        PrefixParts prefixParts = formatPrefix(prefix, bedrockSafe);
        String cleanAlias = alias != null
                ? applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', alias), bedrockSafe)
                : "";
        String aliasColors = LegacyTextUtil.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) {
            aliasColors = defaultAliasColorCode();
        }
        if (aliasColors == null) aliasColors = "";

        String fmt = config.getString("subtitle.format", "%subtitle%");
        return resolveExternal(fmt.replace("%subtitle%", cleanSubtitle)
                .replace("%alias%", cleanAlias)
                .replace("%aliasC%", aliasColors)
                .replace("%original%", original)
                .replace("%prefix%", prefixParts.formatted())
                .replace("%prefixC%", prefixParts.colors())
                .replace("%reset%", LegacyTextUtil.RESET), placeholderResolver);
    }

    public String colorize(String text, boolean bedrockSafe) {
        if (text == null || text.isEmpty()) return text;
        return LegacyTextUtil.translateAlternateColorCodes('&', applyHexColors(text, bedrockSafe));
    }

    public String formatPrefixOnly(String rawPrefix, boolean bedrockSafe) {
        return formatPrefix(rawPrefix, bedrockSafe).formatted();
    }

    public String formatAliasSuffix(String alias, boolean bedrockSafe) {
        if (alias == null || alias.isBlank()) return "";

        String cleanAlias = applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', alias), bedrockSafe);
        String aliasColors = LegacyTextUtil.getLastColors(cleanAlias);
        if ((aliasColors == null || aliasColors.isEmpty()) && !cleanAlias.isEmpty()) {
            aliasColors = defaultAliasColorCode();
        }
        if (aliasColors == null) aliasColors = "";
        return aliasColors + "[" + cleanAlias + "]" + LegacyTextUtil.RESET;
    }

    public boolean hasColorCodes(String text) {
        if (text == null || text.isEmpty()) return false;
        if (HEX_COLOR_PATTERN.matcher(text).find()) return true;
        String legacy = convertHexToLegacy(text);
        return !Objects.equals(LegacyTextUtil.stripColor(legacy), legacy);
    }

    public String stripColor(String text) {
        return LegacyTextUtil.stripColor(text);
    }

    public String applyHexColors(String text, boolean bedrockSafe) {
        if (text == null || text.isBlank()) return text;

        String normalized = normalizeLegacyHex(text);
        if (bedrockSafe && config.getBoolean("bedrock.hexDownsample", true)) {
            return downsampleHexColors(normalized);
        }

        Matcher matcher = HEX_COLOR_PATTERN.matcher(normalized);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(hexToLegacy(matcher.group())));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public String defaultSubtitleColorCode() {
        return LegacyTextUtil.namedColorCode(
                config.getString("subtitle.defaultColor",
                        config.getString("defaultSubtitleColor",
                                config.getString("defaultAliasColor", "AQUA"))),
                "AQUA");
    }

    public String defaultAliasColorCode() {
        return LegacyTextUtil.namedColorCode(config.getString("defaultAliasColor", "AQUA"), "AQUA");
    }

    public String defaultPrefixColorCode() {
        return LegacyTextUtil.namedColorCode(
                config.getString("defaultPrefixColor", config.getString("defaultAliasColor", "AQUA")),
                "AQUA");
    }

    private PrefixParts formatPrefix(String rawPrefix, boolean bedrockSafe) {
        if (rawPrefix == null || rawPrefix.isBlank()) {
            return new PrefixParts("", "");
        }

        String cleanPrefix = applyHexColors(LegacyTextUtil.translateAlternateColorCodes('&', rawPrefix), bedrockSafe);
        String prefixColors = LegacyTextUtil.getLastColors(cleanPrefix);
        if ((prefixColors == null || prefixColors.isEmpty()) && !cleanPrefix.isEmpty()) {
            prefixColors = defaultPrefixColorCode();
        }
        if (prefixColors == null) prefixColors = "";

        String fmt = config.getString("prefixFormat", "%prefixC%%prefix%%reset% ");
        String formatted = fmt.replace("%prefix%", cleanPrefix)
                .replace("%prefixC%", prefixColors)
                .replace("%reset%", LegacyTextUtil.RESET);
        return new PrefixParts(formatted, prefixColors);
    }

    private String resolveExternal(String text, UnaryOperator<String> placeholderResolver) {
        if (placeholderResolver == null || text == null || text.isEmpty()) return text;
        return placeholderResolver.apply(text);
    }

    private String hexToLegacy(String hex) {
        StringBuilder out = new StringBuilder("\u00A7x");
        for (char c : hex.substring(1).toCharArray()) {
            out.append('\u00A7').append(c);
        }
        return out.toString();
    }

    private String normalizeLegacyHex(String text) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String sequence = matcher.group();
            StringBuilder hex = new StringBuilder("#");
            for (int i = 2; i < sequence.length(); i += 2) {
                hex.append(sequence.charAt(i + 1));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(hex.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String downsampleHexColors(String text) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    LegacyTextUtil.nearestLegacyColor(hex.substring(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String convertHexToLegacy(String text) {
        if (text == null || text.isEmpty()) return text;

        Matcher matcher = HEX_COLOR_PATTERN.matcher(normalizeLegacyHex(text));
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(hexToLegacy(matcher.group())));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private record PrefixParts(String formatted, String colors) {
    }
}
