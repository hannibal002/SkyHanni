package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationHandler {

    private static Locations currentLocation = Locations.NONE;
    private static final List<String> UndocumentedLocations = new ArrayList<>();


    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event){
        if (event.rawLine.contains("\u23E3")) {
            String objectiveName = event.objective.getDisplayName().replaceAll("(?i)\\u00A7.", "");
            if (objectiveName.toLowerCase(Locale.ENGLISH).endsWith("guest")){
                LocationHandler.setCurrentLocation(Locations.GUESTISLAND);
            }else {
                LocationHandler.handleLocation(event.formattedLine);
            }
        }
    }

    public static void setCurrentLocation(String location){
        currentLocation = Locations.get(location);
    }

    public static void setCurrentLocation(Locations location){
        currentLocation = location;
    }

    public static Locations getCurrentLocation(){ return currentLocation; }

    public static void handleLocation(String locationLine){
        String location = locationLine.replace(" ", "").toUpperCase(Locale.ENGLISH).trim();
        if (location.startsWith("THECATACOMBS")){
            currentLocation = Locations.CATACOMBS;
        }
        else setCurrentLocation(location.replaceAll("[^A-Za-z0-9]", ""));
    }


    public static void reportUndocumentedLocation(String locationId){
        if (!UndocumentedLocations.contains(locationId)){
            UndocumentedLocations.add(locationId);
            System.out.println("Missing Location value for: " + locationId);
        }
    }

}
