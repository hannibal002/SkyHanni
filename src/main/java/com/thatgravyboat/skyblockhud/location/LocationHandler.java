package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.LocationChangeEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;

public class LocationHandler {

    private static Locations currentLocation = Locations.NONE;

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (event.rawLine.contains("\u23E3")) {
            String objectiveName = event.objective.getDisplayName().replaceAll("(?i)\\u00A7.", "");
            if (objectiveName.toLowerCase(Locale.ENGLISH).endsWith("guest")) {
                LocationHandler.setCurrentLocation(Locations.GUESTISLAND);
            } else {
                LocationHandler.handleLocation(event.formattedLine);
            }
        }
    }

    public static void setCurrentLocation(Locations location) {
        currentLocation = location;
    }

    public static Locations getCurrentLocation() {
        return currentLocation;
    }

    public static void handleLocation(String locationLine) {
        String location = locationLine.replace(" ", "").toUpperCase(Locale.ENGLISH).trim();
        if (location.startsWith("THECATACOMBS")) {
            MinecraftForge.EVENT_BUS.post(new LocationChangeEvent(currentLocation, Locations.CATACOMBS));
            currentLocation = Locations.CATACOMBS;
        } else {
            Locations locations = Locations.get(location.replaceAll("[^A-Za-z0-9]", ""));
            MinecraftForge.EVENT_BUS.post(new LocationChangeEvent(currentLocation, locations));
            currentLocation = locations;
        }
    }
}
