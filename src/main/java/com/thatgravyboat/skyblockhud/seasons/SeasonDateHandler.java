package com.thatgravyboat.skyblockhud.seasons;

import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Pattern;

public class SeasonDateHandler {

    private static Season currentSeason = Season.ERROR;
    private static int currentDate = 1;
    private static String currentEvent = "";
    private static String eventTime = "";


    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event){
        if (Season.get(SeasonDateHandler.removeDate(event.formattedLine.toLowerCase()).toUpperCase()) != Season.ERROR) {
            SeasonDateHandler.setCurrentDateAndSeason(SeasonDateHandler.removeSeason(Utils.removeColor(event.formattedLine.toLowerCase().trim())), SeasonDateHandler.removeDate(Utils.removeColor(event.formattedLine.toLowerCase().trim())).toUpperCase());
        }
    }

    public static void setCurrentDateAndSeason(int date, String season){
        currentDate = date;
        currentSeason = Season.get(season);
    }
    public static void setCurrentEvent(String event, String time){
        currentEvent = event;
        eventTime = time;
    }


    public static Season getCurrentSeason(){ return currentSeason; }
    public static int getCurrentDate(){ return currentDate; }
    private static String getDataSuffix(int date) {
        if (date > 10 && date < 14) return "th";
        switch (date % 10){
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    public static String getFancySeasonAndDate(){ return currentSeason.getDisplayName() + " " + currentDate + getDataSuffix(currentDate); }
    public static String getCurrentEvent() { return currentEvent; }
    public static String getCurrentEventTime() { return eventTime; }

    public static String removeDate(String seasonDate){
        return Pattern.compile("[^a-zA-Z]").matcher(seasonDate.toLowerCase()).replaceAll("").replaceAll("st|nd|rd|th", "").trim();
    }

    public static int removeSeason(String seasonDate){
        return Integer.parseInt(Pattern.compile("[^0-9]").matcher(seasonDate.toLowerCase()).replaceAll("").trim());
    }
}
