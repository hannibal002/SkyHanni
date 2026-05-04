package at.hannibal2.skyhanni.mixins.hooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

public final class ExtendedColorHook {
    private static final char COLOR_CODE = '\u00a7';
    private static final int RGB_HEX_DIGITS = 6;
    private static final int ARGB_HEX_DIGITS = 8;
    private static final String ARGB_COLOR_NAME_PREFIX = "skyhanni_argb_";

    private ExtendedColorHook() {
    }

    public static boolean shouldSkipLegacyFormatting(String text, int sectionIndex, char colorCode) {
        if (colorCode == '#') {
            return true;
        }

        int precedingHexDigits = countPrecedingHexDigits(text, sectionIndex);
        if (colorCode == '/') {
            return isCompleteColor(precedingHexDigits);
        }

        return precedingHexDigits >= 0 && precedingHexDigits < ARGB_HEX_DIGITS && isHexDigit(colorCode);
    }

    public static Style applyExtendedColorStyle(Style style, String text, int sectionIndex, char colorCode) {
        if (colorCode != '/') {
            return style;
        }

        int hexDigits = countPrecedingHexDigits(text, sectionIndex);
        if (!isCompleteColor(hexDigits)) {
            return style;
        }

        return style.applyLegacyFormat(ChatFormatting.WHITE).withColor(createTextColor(readColor(text, sectionIndex, hexDigits), hexDigits));
    }

    public static int applyExtendedColorAlpha(int originalColor, TextColor textColor) {
        if (textColor == null || textColor.name == null || !textColor.name.startsWith(ARGB_COLOR_NAME_PREFIX)) {
            return originalColor;
        }

        String argbColor = textColor.name.substring(ARGB_COLOR_NAME_PREFIX.length());
        if (argbColor.length() != ARGB_HEX_DIGITS) {
            return originalColor;
        }

        try {
            int alpha = Integer.parseInt(argbColor.substring(0, 2), 16);
            return (alpha << 24) | (originalColor & 0xFFFFFF);
        } catch (NumberFormatException ignored) {
            return originalColor;
        }
    }

    private static boolean isCompleteColor(int hexDigits) {
        return hexDigits == RGB_HEX_DIGITS || hexDigits == ARGB_HEX_DIGITS;
    }

    private static int countPrecedingHexDigits(String text, int sectionIndex) {
        int hexDigits = 0;

        for (int index = sectionIndex - 2; index >= 0; index -= 2) {
            if (text.charAt(index) != COLOR_CODE || index + 1 >= text.length()) {
                return -1;
            }

            char colorCode = text.charAt(index + 1);
            if (colorCode == '#') {
                return hexDigits;
            }
            if (!isHexDigit(colorCode)) {
                return -1;
            }

            hexDigits++;
            if (hexDigits > ARGB_HEX_DIGITS) {
                return -1;
            }
        }

        return -1;
    }

    private static TextColor createTextColor(int color, int hexDigits) {
        int rgb = color & 0xFFFFFF;
        if (hexDigits == ARGB_HEX_DIGITS) {
            return new TextColor(rgb, ARGB_COLOR_NAME_PREFIX + String.format(Locale.ROOT, "%08x", color));
        }

        return TextColor.fromRgb(rgb);
    }

    private static int readColor(String text, int sectionIndex, int hexDigits) {
        int color = 0;
        int startIndex = sectionIndex - hexDigits * 2;

        for (int index = startIndex; index < sectionIndex; index += 2) {
            color = (color << 4) | Character.digit(text.charAt(index + 1), 16);
        }

        return color;
    }

    private static boolean isHexDigit(char colorCode) {
        return Character.digit(colorCode, 16) >= 0;
    }
}
