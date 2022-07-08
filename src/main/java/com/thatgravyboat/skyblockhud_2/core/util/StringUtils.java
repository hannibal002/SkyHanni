package com.thatgravyboat.skyblockhud_2.core.util;

public class StringUtils {

    public static String cleanColour(String in) {
        return in.replaceAll("(?i)\\u00A7.", "");
    }
}
