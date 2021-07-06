package com.thatgravyboat.skyblockhud.handlers;

import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

public class TimeHandler {

    public static long time;

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (Pattern.matches("([0-9]*):([0-9]*)(pm|am)", event.formattedLine.toLowerCase().trim())) {
            boolean isPm = event.formattedLine.toLowerCase().trim().endsWith("pm");
            SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a", Locale.CANADA);
            String currentTimeString = event.formattedLine
                .replace(" ", "")
                .replace(isPm ? "pm" : "am", isPm ? " pm" : " am");
            try {
                time =
                    (parseFormat.parse(currentTimeString).getTime() - parseFormat.parse("00:00 am").getTime()) / 1000L;
            } catch (ParseException ignored) {
                LogManager.getLogger().warn("timeformat error: " + currentTimeString);
            }
        }
    }
}
