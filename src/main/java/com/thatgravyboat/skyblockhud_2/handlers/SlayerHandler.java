package com.thatgravyboat.skyblockhud_2.handlers;

import com.thatgravyboat.skyblockhud_2.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud_2.api.events.SidebarPostEvent;
import com.thatgravyboat.skyblockhud_2.api.events.SkyBlockEntityKilled;
import com.thatgravyboat.skyblockhud_2.utils.Utils;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SlayerHandler {

    private static final Pattern KILLS_REGEX = Pattern.compile("(\\d+)/(\\d+) kills?");
    private static final Pattern SLAYER_PATTERN = Pattern.compile("Talk to Maddox to claim your ([A-Za-z]+) Slayer XP!");

    public enum slayerTypes {
        ZOMBIE(34, "Revenant Horror"),
        WOLF(42, "Sven Packmaster"),
        SPIDER(50, "Tarantula Broodfather"),
        VOIDGLOOMSERAPH(58, "Voidgloom Seraph"),
        NONE(0, "");

        private final String displayName;
        private final int x;

        slayerTypes(int x, String displayName) {
            this.displayName = displayName;
            this.x = x;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getX() {
            return x;
        }
    }

    public static slayerTypes currentSlayer = slayerTypes.NONE;
    public static int slayerTier = 0;
    public static boolean isDoingSlayer = false;
    public static int progress = 0;
    public static int maxKills = 0;
    public static boolean bossSlain = false;
    public static boolean isKillingBoss = false;

    public static void clearSlayer() {
        currentSlayer = slayerTypes.NONE;
        isDoingSlayer = false;
        progress = 0;
        maxKills = 0;
        bossSlain = false;
        isKillingBoss = false;
    }

    @SubscribeEvent
    public void onSidebarPost(SidebarPostEvent event) {
        String arrayString = Arrays.toString(event.arrayScores);
        isDoingSlayer = Arrays.toString(event.arrayScores).contains("Slayer Quest");
        if (isDoingSlayer && (currentSlayer.equals(slayerTypes.NONE) || !arrayString.replace(" ", "").contains(currentSlayer.getDisplayName().replace(" ", "") + Utils.intToRomanNumeral(slayerTier)))) {
            for (int i = 0; i < event.scores.size(); i++) {
                String line = event.scores.get(i);
                if (line.contains("Slayer Quest") && event.scores.size() > 3) {
                    String slayer = event.scores.get(i - 1).toLowerCase();
                    SlayerHandler.slayerTypes selectedSlayer = SlayerHandler.slayerTypes.NONE;
                    for (slayerTypes types : slayerTypes.values()) {
                        if (slayer.contains(types.displayName.toLowerCase(Locale.ENGLISH))) {
                            selectedSlayer = types;
                            break;
                        }
                    }
                    SlayerHandler.currentSlayer = selectedSlayer;
                    SlayerHandler.slayerTier = Utils.whatRomanNumeral(slayer.replace(selectedSlayer.getDisplayName().toLowerCase(), "").replace(" ", ""));
                    break;
                }
            }
        }

        if (!isDoingSlayer) {
            clearSlayer();
        }
    }

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (!isDoingSlayer && event.formattedLine.equals("Slayer Quest")) isDoingSlayer = true;

        if (isDoingSlayer) {
            String line = event.formattedLine.toLowerCase();
            Matcher killMatcher = KILLS_REGEX.matcher(line);

            if (killMatcher.find()) {
                SlayerHandler.bossSlain = false;
                SlayerHandler.isKillingBoss = false;
                try {
                    progress = Integer.parseInt(killMatcher.group(1));
                } catch (Exception ignored) {}
                try {
                    maxKills = Integer.parseInt(killMatcher.group(2));
                } catch (Exception ignored) {}
            } else if (line.contains("slay the boss")) {
                SlayerHandler.bossSlain = false;
                SlayerHandler.isKillingBoss = true;
                SlayerHandler.maxKills = 0;
                SlayerHandler.progress = 0;
            } else if (line.contains("boss slain")) {
                SlayerHandler.isKillingBoss = false;
                SlayerHandler.maxKills = 0;
                SlayerHandler.progress = 0;
                SlayerHandler.bossSlain = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (event.type != 2) {
            Matcher slayerMatcher = SLAYER_PATTERN.matcher(Utils.removeColor(event.message.getUnformattedText()));
            if (slayerMatcher.find()) {
                MinecraftForge.EVENT_BUS.post(new SkyBlockEntityKilled(slayerMatcher.group(1).toUpperCase(Locale.ENGLISH) + "_SLAYER", null));
            }
        }
    }
}
