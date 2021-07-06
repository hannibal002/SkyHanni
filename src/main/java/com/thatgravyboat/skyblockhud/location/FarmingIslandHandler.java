package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import java.util.Arrays;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FarmingIslandHandler {

    public static Locations location = Locations.NONE;
    public static int pelts;

    @SubscribeEvent
    public void onSidebarPost(SidebarPostEvent event) {
        boolean isTracking = Arrays.toString(event.arrayScores).toLowerCase().contains("tracker mob location:");
        if (isTracking && location == Locations.NONE) {
            for (int i = 0; i < event.scores.size(); i++) {
                String line = event.scores.get(i);
                if (line.toLowerCase().contains("tracker mob location:") && i > 2) {
                    location = Locations.get(event.scores.get(i - 1).toLowerCase());
                    break;
                }
            }
        }
        if (!isTracking && location != Locations.NONE) {
            location = Locations.NONE;
        }
    }
}
