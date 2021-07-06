package com.thatgravyboat.skyblockhud.dungeons;

import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.SidebarLineUpdateEvent;
import com.thatgravyboat.skyblockhud.api.events.SidebarPostEvent;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DungeonHandler {

    private static final HashMap<String, DungeonPlayer> dungeonPlayersMap = new HashMap<>();
    private static int dungeonTime = 0;
    private static int dungeonCleared = 0;
    private static boolean bloodKey = false;
    private static int witherKeys = 0;
    private static int maxSecrets = 0;
    private static int secrets = 0;
    private static int totalSecrets = 0;
    private static int deaths = 0;
    private static int crypts = 0;

    private static final Pattern DungeonPlayerRegex = Pattern.compile("^\\[([HMBAT])] ([\\w]+) ([0-9]+|DEAD)$");

    @SubscribeEvent
    public void onSidebarLineUpdate(SidebarLineUpdateEvent event) {
        if (LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS)) {
            DungeonHandler.checkForDungeonTime(event.formattedLine);
            DungeonHandler.checkForDungeonCleared(event.formattedLine);
            DungeonHandler.checkForDungeonKeys(event.formattedLine, event.rawLine);
            DungeonHandler.checkForDungeonPlayers(event.formattedLine, Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent
    public void onSidebarPost(SidebarPostEvent event) {
        if (!LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS)) {
            DungeonHandler.clearDungeonStats();
        }
    }

    public static void checkForDungeonPlayers(String scoreLine, Minecraft mc) {
        Matcher dungeonMatcher = DungeonPlayerRegex.matcher(scoreLine);
        if (dungeonMatcher.matches() && DungeonHandler.dungeonTime > 0) {
            Classes playerClass = Classes.valueOf(dungeonMatcher.group(1));
            String displayName = dungeonMatcher.group(2);
            String health = dungeonMatcher.group(3);
            if (!mc.thePlayer.getName().toLowerCase().startsWith(displayName.toLowerCase().trim())) {
                int healthNum = 0;
                if (!health.equalsIgnoreCase("dead")) {
                    try {
                        healthNum = Integer.parseInt(health);
                    } catch (NumberFormatException ignored) {}
                }
                DungeonPlayer player = new DungeonPlayer(playerClass, displayName, healthNum, health.equalsIgnoreCase("dead"));
                dungeonPlayersMap.put(displayName.toLowerCase(), player);
            }
        }
    }

    public static void checkForDungeonTime(String scoreLine) {
        if (scoreLine.toLowerCase().trim().contains("time elapsed:")) {
            String timeLine = scoreLine.toLowerCase().trim().replace("time elapsed:", "");
            String[] times = timeLine.split("m ");
            int time = 0;
            try {
                time += Integer.parseInt(times[0].replace(" ", "").replace("m", "")) * 60;
                time += Integer.parseInt(times[1].replace(" ", "").replace("s", ""));
            } catch (NumberFormatException ignored) {}
            dungeonTime = time;
        }
    }

    public static void checkForDungeonCleared(String scoreline) {
        if (scoreline.toLowerCase().trim().contains("dungeon cleared:")) {
            String dungeonClearedText = scoreline.toLowerCase().trim().replace("dungeon cleared:", "").replace(" ", "").replace("%", "");
            try {
                dungeonCleared = Integer.parseInt(dungeonClearedText);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void checkForDungeonKeys(String scoreline, String rawString) {
        if (scoreline.toLowerCase().trim().contains("keys:")) {
            String dungeonClearedText = scoreline.toLowerCase().trim().replace("keys:", "").replace(" ", "").replace("x", "");
            bloodKey = rawString.contains("\u2713");
            try {
                witherKeys = Integer.parseInt(dungeonClearedText);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void parseSecrets(String statusBar) {
        boolean hasSecrets = false;
        String[] parts = statusBar.split(" {4,}");
        for (String part : parts) {
            if (part.toLowerCase().contains("secrets") && !statusBar.toLowerCase().contains("no secrets")) {
                hasSecrets = true;
                try {
                    String secret = Utils.removeColor(part.replace("Secrets", "")).replace(" ", "");
                    maxSecrets = Integer.parseInt(secret.split("/")[1]);
                    secrets = Integer.parseInt(secret.split("/")[0]);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (!hasSecrets) {
            maxSecrets = 0;
            secrets = 0;
        }
    }

    public static void parseTotalSecrets(String playerName) {
        if (playerName.toLowerCase().contains("secrets found:")) {
            String totalSecret = Utils.removeColor(playerName.toLowerCase().replace("secrets found:", "")).replace(" ", "");
            try {
                totalSecrets = Integer.parseInt(totalSecret);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void parseDeaths(String playerName) {
        if (playerName.toLowerCase().contains("deaths:")) {
            String death = Utils.removeColor(playerName.toLowerCase().replace("deaths:", "")).replace("(", "").replace(")", "").replace(" ", "");
            try {
                deaths = Integer.parseInt(death);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void parseCrypts(String playerName) {
        if (playerName.toLowerCase().contains("crypts:")) {
            String crypt = Utils.removeColor(playerName.toLowerCase().replace("crypts:", "")).replace(" ", "");
            try {
                crypts = Integer.parseInt(crypt);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void clearDungeonStats() {
        dungeonPlayersMap.clear();
        dungeonTime = 0;
        dungeonCleared = 0;
        bloodKey = false;
        witherKeys = 0;
        maxSecrets = 0;
        secrets = 0;
        totalSecrets = 0;
        deaths = 0;
        crypts = 0;
    }

    public static HashMap<String, DungeonPlayer> getDungeonPlayers() {
        return dungeonPlayersMap;
    }

    public static int getDungeonTime() {
        return dungeonTime;
    }

    public static int getDungeonCleared() {
        return dungeonCleared;
    }

    public static int getWitherKeys() {
        return witherKeys;
    }

    public static boolean hasBloodkey() {
        return bloodKey;
    }

    public static int getMaxSecrets() {
        return maxSecrets;
    }

    public static int getSecrets() {
        return secrets;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getTotalSecrets() {
        return totalSecrets;
    }

    public static int getCrypts() {
        return crypts;
    }
}
