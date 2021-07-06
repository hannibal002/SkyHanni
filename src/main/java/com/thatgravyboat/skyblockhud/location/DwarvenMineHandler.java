package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DwarvenMineHandler {

    public enum Event {
        NONE(0, "Unknown"),
        TICKET(107, "Raffle"),
        GOBLIN(99, "Goblin Raid");

        public int x;
        public String displayName;

        Event(int x, String displayName) {
            this.x = x;
            this.displayName = displayName;
        }
    }

    public static int mithril;

    public static int eventMax;
    public static int eventProgress;
    public static Event currentEvent;

    private static final DecimalFormat formatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.CANADA));

    public static String getMithrilFormatted() {
        String output = formatter.format(mithril);
        if (output.equals(".0")) output = "0.0"; else if (output.equals(",0")) output = "0,0";
        return output;
    }

    public static void parseMithril(String line) {
        try {
            mithril = Integer.parseInt(line.toLowerCase().replace("mithril powder:", "").trim());
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (event.formattedLine.toLowerCase().contains("mithril")) {
            try {
                mithril = Integer.parseInt(event.formattedLine.toLowerCase().replace("mithril:", "").trim());
            } catch (Exception ignored) {}
        }
        if (event.formattedLine.toLowerCase().contains("event")) {
            if (event.formattedLine.toLowerCase().contains("raffle")) {
                DwarvenMineHandler.currentEvent = Event.TICKET;
            } else if (event.formattedLine.toLowerCase().contains("goblin raid")) {
                DwarvenMineHandler.currentEvent = Event.GOBLIN;
            }
        }
        if (DwarvenMineHandler.currentEvent != Event.NONE) {
            if (DwarvenMineHandler.currentEvent == Event.TICKET && event.formattedLine.toLowerCase().contains("tickets:")) {
                if (event.formattedLine.toLowerCase().contains("pool:")) {
                    try {
                        eventMax = Integer.parseInt(event.formattedLine.toLowerCase().replace("pool:", "").trim().split("/")[0].trim());
                    } catch (Exception ignored) {}
                } else if (event.formattedLine.toLowerCase().contains("tickets:")) {
                    try {
                        eventProgress = Integer.parseInt(event.formattedLine.toLowerCase().replace("tickets:", "").split("\\(")[0].trim());
                    } catch (Exception ignored) {}
                }
            } else if (DwarvenMineHandler.currentEvent == Event.GOBLIN) {
                if (event.formattedLine.toLowerCase().contains("remaining:")) {
                    try {
                        eventMax =
                            Integer.parseInt(event.formattedLine.toLowerCase().replace("goblins", "").replace("remaining:", "").trim());
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
        if (!arrayString.toLowerCase().contains("event:")) {
            DwarvenMineHandler.currentEvent = Event.NONE;
            DwarvenMineHandler.eventProgress = 0;
            DwarvenMineHandler.eventMax = 0;
        }
    }
}
