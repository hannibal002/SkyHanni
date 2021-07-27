package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import com.thatgravyboat.skyblockhud.overlay.MiningHud;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MinesHandler {

    public enum Event {
        NONE(0, "Unknown", false, false),
        TICKET(107, "Raffle", true, true),
        GOBLIN(99, "Goblin Raid", true, true),
        WIND(0, "Gone With The Wind", false, false),
        TOGETHER(171, "Better Together", false, true);

        public int x;
        public String displayName;
        public boolean needsMax;
        public boolean display;

        Event(int x, String displayName, boolean needsMax, boolean display) {
            this.x = x;
            this.displayName = displayName;
            this.needsMax = needsMax;
            this.display = display;
        }
    }

    public static int mithril;
    public static int gemstone;

    public static int eventMax;
    public static int eventProgress;
    public static Event currentEvent;

    private static final DecimalFormat NORMAL_FORMATTER = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.CANADA));
    private static final DecimalFormat SHORT_FORMATTER = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.CANADA));

    static {
        SHORT_FORMATTER.setRoundingMode(RoundingMode.FLOOR);
    }

    public static String getMithrilFormatted() {
        String output = NORMAL_FORMATTER.format(mithril);
        if (output.equals(".0")) output = "0.0"; else if (output.equals(",0")) output = "0,0";
        return output;
    }

    public static String getMithrilShortFormatted() {
        return mithril > 999 ? SHORT_FORMATTER.format((double) mithril / 1000) + "k" : String.valueOf(mithril);
    }

    public static String getGemstoneFormatted() {
        String output = NORMAL_FORMATTER.format(gemstone);
        if (output.equals(".0")) output = "0.0"; else if (output.equals(",0")) output = "0,0";
        return output;
    }

    public static String getGemstoneShortFormatted() {
        return gemstone > 999 ? SHORT_FORMATTER.format((double) gemstone / 1000) + "k" : String.valueOf(gemstone);
    }

    public static void parseMithril(String line) {
        try {
            mithril = Integer.parseInt(line.toLowerCase().replace("mithril powder:", "").trim());
        } catch (Exception ignored) {}
    }

    public static void parseGemstone(String line) {
        try {
            gemstone = Integer.parseInt(line.toLowerCase().replace("gemstone powder:", "").trim());
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (event.formattedLine.toLowerCase().contains("heat")) {
            try {
                MiningHud.setHeat(Integer.parseInt(event.formattedLine.toLowerCase().replace("heat:", "").trim()));
            } catch (Exception ignored) {}
        }
        if (event.formattedLine.toLowerCase().contains("mithril")) {
            try {
                mithril = Integer.parseInt(event.formattedLine.toLowerCase().replace("mithril:", "").trim());
            } catch (Exception ignored) {}
        }
        if (event.formattedLine.toLowerCase().contains("gemstone")) {
            try {
                gemstone = Integer.parseInt(event.formattedLine.toLowerCase().replace("gemstone:", "").trim());
            } catch (Exception ignored) {}
        }
        if (event.formattedLine.toLowerCase().contains("event")) {
            if (event.formattedLine.toLowerCase().contains("raffle")) {
                MinesHandler.currentEvent = Event.TICKET;
            } else if (event.formattedLine.toLowerCase().contains("goblin raid")) {
                MinesHandler.currentEvent = Event.GOBLIN;
            }
        }
        if (event.formattedLine.equalsIgnoreCase("wind compass")) {
            MinesHandler.currentEvent = Event.WIND;
        }
        if (event.formattedLine.toLowerCase(Locale.ENGLISH).contains("nearby players")) {
            MinesHandler.currentEvent = Event.TOGETHER;
            try {
                MinesHandler.eventProgress = Integer.parseInt(event.formattedLine.toLowerCase().replace("nearby players:", "").trim());
            } catch (Exception ignored) {}
        }

        if (MinesHandler.currentEvent != Event.NONE) {
            if (MinesHandler.currentEvent == Event.TICKET) {
                if (event.formattedLine.toLowerCase().contains("pool:")) {
                    try {
                        eventMax = Integer.parseInt(event.formattedLine.toLowerCase().replace("pool:", "").trim().split("/")[0].trim());
                    } catch (Exception ignored) {}
                } else if (event.formattedLine.toLowerCase().contains("tickets:")) {
                    try {
                        eventProgress = Integer.parseInt(event.formattedLine.toLowerCase().replace("tickets:", "").split("\\(")[0].trim());
                    } catch (Exception ignored) {}
                }
            } else if (MinesHandler.currentEvent == Event.GOBLIN) {
                if (event.formattedLine.toLowerCase().contains("remaining:")) {
                    try {
                        eventMax = Integer.parseInt(event.formattedLine.toLowerCase().replace("goblins", "").replace("remaining:", "").trim());
                    } catch (Exception ignored) {}
                } else if (event.formattedLine.toLowerCase().contains("your kills:") && !event.formattedLine.toLowerCase().contains("(")) {
                    try {
                        eventProgress = Integer.parseInt(event.formattedLine.toLowerCase().replace("your kills:", "").trim());
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @SubscribeEvent
    public void onSidebarPost(SidebarPostEvent event) {
        String arrayString = Arrays.toString(event.arrayScores);
        boolean hasEvent = arrayString.toLowerCase().contains("event:");
        boolean hasWind = arrayString.toLowerCase().contains("wind compass");
        boolean hasNearbyPlayers = arrayString.toLowerCase().contains("nearby players");

        if (!hasEvent && !hasWind && !hasNearbyPlayers) {
            MinesHandler.currentEvent = Event.NONE;
            MinesHandler.eventProgress = 0;
            MinesHandler.eventMax = 0;
        }
        if (!arrayString.toLowerCase().contains("heat:")) {
            MiningHud.setHeat(0);
        }
    }
}
