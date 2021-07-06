package com.thatgravyboat.skyblockhud.location;

import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.ProfileSwitchedEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import com.thatgravyboat.skyblockhud.handlers.CurrencyHandler;
import java.util.Arrays;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class IslandHandler {

  public static int flightTime;
  public static boolean hadFlightTime;

  public static int redstone;
  public static boolean hadRedstone;

  @SubscribeEvent
  public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
    hadFlightTime = checkFlightDuration(event.formattedLine);
    hadRedstone = checkRestone(event.formattedLine);
  }

  @SubscribeEvent
  public void onProfileSwitch(ProfileSwitchedEvent event) {
    flightTime = 0;
  }

  public static boolean checkFlightDuration(String formatedScoreboardLine) {
    if (
      LocationHandler.getCurrentLocation() == Locations.YOURISLAND &&
      Utils
        .removeColor(formatedScoreboardLine.toLowerCase().trim())
        .contains("flight duration:")
    ) {
      String timeString = formatedScoreboardLine
        .toLowerCase()
        .replace("flight duration:", "")
        .replace(" ", "");
      String[] times = timeString.split(":");
      if (times.length == 2) {
        int s = 0;
        try {
          s += Integer.parseInt(times[0]) * 60;
        } catch (NumberFormatException ignored) {}
        try {
          s += Integer.parseInt(times[1]);
        } catch (NumberFormatException ignored) {}
        flightTime = s - 1;
      } else if (times.length == 3) {
        int s = 0;
        try {
          s += Integer.parseInt(times[0]) * 3600;
        } catch (NumberFormatException ignored) {}
        try {
          s += Integer.parseInt(times[1]) * 60;
        } catch (NumberFormatException ignored) {}
        try {
          s += Integer.parseInt(times[2]);
        } catch (NumberFormatException ignored) {}
        flightTime = s - 1;
      }
      return true;
    }
    return false;
  }

  public static boolean checkRestone(String formatedScoreboardLine) {
    if (LocationHandler.getCurrentLocation() == Locations.YOURISLAND) {
      if (
        formatedScoreboardLine.toLowerCase().contains("redstone:")
      ) return true;
      try {
        redstone =
          formatedScoreboardLine.toLowerCase().contains("redstone:")
            ? Integer.parseInt(
              Utils.removeWhiteSpaceAndRemoveWord(
                formatedScoreboardLine,
                "redstone:"
              )
            )
            : 0;
      } catch (Exception ignored) {}
    }
    return false;
  }
}
