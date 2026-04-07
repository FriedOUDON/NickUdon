package com.FriedOUDON.NickUdon.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class LegacyTextUtil {
    public static final char COLOR_CHAR = '\u00A7';
    public static final String RESET = "\u00A7r";

    private static final String COLOR_CODE_CHARS = "0123456789abcdef";
    private static final String FORMAT_CODE_CHARS = "klmno";
    private static final Pattern STRIP_COLOR_PATTERN =
            Pattern.compile("(?i)\u00A7x(\u00A7[0-9A-F]){6}|\u00A7[0-9A-FK-OR]");

    private static final Map<String, String> NAMED_COLORS = buildNamedColors();

    private static final String[] LEGACY_COLOR_CODES = new String[]{
            "\u00A70", "\u00A71", "\u00A72", "\u00A73",
            "\u00A74", "\u00A75", "\u00A76", "\u00A77",
            "\u00A78", "\u00A79", "\u00A7a", "\u00A7b",
            "\u00A7c", "\u00A7d", "\u00A7e", "\u00A7f"
    };

    private static final int[] LEGACY_RGB = new int[]{
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private LegacyTextUtil() {
    }

    public static String translateAlternateColorCodes(char altColorChar, String text) {
        if (text == null || text.isEmpty()) return text;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && isLegacyCode(chars[i + 1])) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) return text;
        return STRIP_COLOR_PATTERN.matcher(text).replaceAll("");
    }

    public static String getLastColors(String text) {
        if (text == null || text.isEmpty()) return "";

        String currentColor = "";
        List<String> activeFormats = new ArrayList<>();

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != COLOR_CHAR) continue;

            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'x' && i + 13 < text.length()) {
                String hex = text.substring(i, i + 14);
                currentColor = hex;
                activeFormats.clear();
                i += 13;
                continue;
            }

            String escaped = "" + COLOR_CHAR + code;
            if (isColorCode(code)) {
                currentColor = escaped;
                activeFormats.clear();
            } else if (isFormatCode(code)) {
                activeFormats.add(escaped);
            } else if (code == 'r') {
                currentColor = "";
                activeFormats.clear();
            }
        }

        StringBuilder out = new StringBuilder(currentColor);
        for (String format : activeFormats) {
            out.append(format);
        }
        return out.toString();
    }

    public static String namedColorCode(String colorName, String fallbackName) {
        String normalized = colorName == null ? "" : colorName.toUpperCase(Locale.ROOT);
        String fallback = fallbackName == null ? "AQUA" : fallbackName.toUpperCase(Locale.ROOT);
        return NAMED_COLORS.getOrDefault(normalized, NAMED_COLORS.getOrDefault(fallback, "\u00A7b"));
    }

    public static boolean isLegacyCode(char c) {
        char normalized = Character.toLowerCase(c);
        return isColorCode(normalized) || isFormatCode(normalized) || normalized == 'r' || normalized == 'x';
    }

    public static boolean isColorCode(char c) {
        return COLOR_CODE_CHARS.indexOf(Character.toLowerCase(c)) >= 0;
    }

    public static boolean isFormatCode(char c) {
        return FORMAT_CODE_CHARS.indexOf(Character.toLowerCase(c)) >= 0;
    }

    public static String nearestLegacyColor(String rgb) {
        try {
            int r = Integer.parseInt(rgb.substring(0, 2), 16);
            int g = Integer.parseInt(rgb.substring(2, 4), 16);
            int b = Integer.parseInt(rgb.substring(4, 6), 16);
            int bestDistance = Integer.MAX_VALUE;
            String bestCode = "\u00A7f";

            for (int i = 0; i < LEGACY_RGB.length; i++) {
                int lr = (LEGACY_RGB[i] >> 16) & 0xFF;
                int lg = (LEGACY_RGB[i] >> 8) & 0xFF;
                int lb = LEGACY_RGB[i] & 0xFF;

                int dr = r - lr;
                int dg = g - lg;
                int db = b - lb;
                int distance = dr * dr + dg * dg + db * db;

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCode = LEGACY_COLOR_CODES[i];
                }
            }

            return bestCode;
        } catch (Exception ignored) {
            return "\u00A7f";
        }
    }

    private static Map<String, String> buildNamedColors() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("BLACK", "\u00A70");
        out.put("DARK_BLUE", "\u00A71");
        out.put("DARK_GREEN", "\u00A72");
        out.put("DARK_AQUA", "\u00A73");
        out.put("DARK_RED", "\u00A74");
        out.put("DARK_PURPLE", "\u00A75");
        out.put("GOLD", "\u00A76");
        out.put("GRAY", "\u00A77");
        out.put("DARK_GRAY", "\u00A78");
        out.put("BLUE", "\u00A79");
        out.put("GREEN", "\u00A7a");
        out.put("AQUA", "\u00A7b");
        out.put("RED", "\u00A7c");
        out.put("LIGHT_PURPLE", "\u00A7d");
        out.put("YELLOW", "\u00A7e");
        out.put("WHITE", "\u00A7f");
        return out;
    }
}
