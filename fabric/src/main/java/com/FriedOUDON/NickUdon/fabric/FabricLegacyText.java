package com.FriedOUDON.NickUdon.fabric;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

final class FabricLegacyText {
    private FabricLegacyText() {
    }

    static Text parse(String legacy) {
        if (legacy == null || legacy.isEmpty()) return Text.empty();

        MutableText root = Text.empty();
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < legacy.length(); i++) {
            char c = legacy.charAt(i);
            if (c != '\u00A7' || i + 1 >= legacy.length()) {
                buffer.append(c);
                continue;
            }

            flush(root, buffer, style);

            char code = Character.toLowerCase(legacy.charAt(++i));
            if (code == 'x' && i + 12 < legacy.length()) {
                StringBuilder hex = new StringBuilder();
                boolean valid = true;
                for (int j = 0; j < 6; j++) {
                    int markerIndex = i + 1 + j * 2;
                    int valueIndex = i + 2 + j * 2;
                    if (markerIndex >= legacy.length() || valueIndex >= legacy.length()
                            || legacy.charAt(markerIndex) != '\u00A7') {
                        valid = false;
                        break;
                    }
                    hex.append(legacy.charAt(valueIndex));
                }
                if (valid) {
                    try {
                        style = Style.EMPTY.withColor(TextColor.fromRgb(Integer.parseInt(hex.toString(), 16)));
                        i += 12;
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                }
                buffer.append('\u00A7').append(code);
                continue;
            }

            Formatting formatting = switch (code) {
                case '0' -> Formatting.BLACK;
                case '1' -> Formatting.DARK_BLUE;
                case '2' -> Formatting.DARK_GREEN;
                case '3' -> Formatting.DARK_AQUA;
                case '4' -> Formatting.DARK_RED;
                case '5' -> Formatting.DARK_PURPLE;
                case '6' -> Formatting.GOLD;
                case '7' -> Formatting.GRAY;
                case '8' -> Formatting.DARK_GRAY;
                case '9' -> Formatting.BLUE;
                case 'a' -> Formatting.GREEN;
                case 'b' -> Formatting.AQUA;
                case 'c' -> Formatting.RED;
                case 'd' -> Formatting.LIGHT_PURPLE;
                case 'e' -> Formatting.YELLOW;
                case 'f' -> Formatting.WHITE;
                case 'k' -> Formatting.OBFUSCATED;
                case 'l' -> Formatting.BOLD;
                case 'm' -> Formatting.STRIKETHROUGH;
                case 'n' -> Formatting.UNDERLINE;
                case 'o' -> Formatting.ITALIC;
                case 'r' -> Formatting.RESET;
                default -> null;
            };

            if (formatting == null) {
                buffer.append('\u00A7').append(code);
                continue;
            }

            if (formatting == Formatting.RESET) {
                style = Style.EMPTY;
            } else if (formatting.isColor()) {
                style = Style.EMPTY.withFormatting(formatting);
            } else {
                style = style.withFormatting(formatting);
            }
        }

        flush(root, buffer, style);
        return root;
    }

    private static void flush(MutableText root, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) return;
        root.append(Text.literal(buffer.toString()).setStyle(style));
        buffer.setLength(0);
    }
}
