package com.thatgravyboat.skyblockhud.location;

import javax.annotation.Nullable;

public class ParkIslandHandler {

    private static boolean isRaining = false;
    private static String rainTime = "";

    public static void parseRain(@Nullable String tabLine) {
        if (tabLine == null) {
            isRaining = false;
            rainTime = "";
        } else if (tabLine.toLowerCase().contains("rain:")) {
            if (tabLine.toLowerCase().contains("no rain")) isRaining = false; else {
                rainTime = tabLine.toLowerCase().replace("rain:", "").replace(" ", "");
                isRaining = true;
            }
        }
    }

    public static String getRainTime() {
        return rainTime;
    }

    public static boolean isRaining() {
        return isRaining;
    }
}
