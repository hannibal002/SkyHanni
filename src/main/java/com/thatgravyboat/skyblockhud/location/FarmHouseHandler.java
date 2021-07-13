package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.ProfileSwitchedEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import java.util.Arrays;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FarmHouseHandler {

    public enum Medal {
        BRONZE,
        SILVER,
        GOLD
    }

    private static final int[] medals = new int[Medal.values().length];

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (event.formattedLine.contains("medals:")) {
            for (Medal value : Medal.values()) {
                if (event.formattedLine.contains(value.name())) {
                    try {
                        medals[value.ordinal()] = Integer.parseInt(event.formattedLine.replace("medals:", "").replace(value.name(), "").trim());
                    } catch (Exception ignored) {}
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onProfileSwitch(ProfileSwitchedEvent event) {
        Arrays.fill(medals, 0);
    }

    public static String getFormattedMedals(Medal medal) {
        if (medal == null) return "0";
        return String.valueOf(medals[medal.ordinal()]);
    }
}
