package com.thatgravyboat.skyblockhud.core.util;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class StringUtils {

    public static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");

    public static String cleanColour(String in) {
        return in.replaceAll("(?i)\\u00A7.", "");
    }

    public static String cleanColourNotModifiers(String in) {
        return in.replaceAll("(?i)\\u00A7[0-9a-f]", "\u00A7r");
    }

    public static String trimToWidth(String str, int len) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String trim = fr.trimStringToWidth(str, len);

        if (str.length() != trim.length() && !trim.endsWith(" ")) {
            char next = str.charAt(trim.length());
            if (next != ' ') {
                String[] split = trim.split(" ");
                String last = split[split.length - 1];
                if (last.length() < 8) {
                    trim = trim.substring(0, trim.length() - last.length());
                }
            }
        }

        return trim;
    }
}
