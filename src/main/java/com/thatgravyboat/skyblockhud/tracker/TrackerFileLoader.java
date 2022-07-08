package com.thatgravyboat.skyblockhud.tracker;

import com.google.gson.*;
import com.thatgravyboat.skyblockhud.LorenzMod;
import com.thatgravyboat.skyblockhud.location.Locations;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class TrackerFileLoader {

    private static final Gson gson = new GsonBuilder().create();

    private static void loadTrackers(JsonObject object) {
        for (JsonElement element : object.get("trackers").getAsJsonArray()) {
            JsonObject tracker = element.getAsJsonObject();
            EnumSet<Locations> locations = EnumSet.noneOf(Locations.class);
            tracker
                .get("location")
                .getAsJsonArray()
                .forEach(l -> {
                    Locations location = Locations.get(l.getAsString().toUpperCase(Locale.ENGLISH));
                    if (location != Locations.DEFAULT) {
                        locations.add(location);
                    }
                });
            if (tracker.has("drops")) {
                for (JsonElement drop : tracker.get("drops").getAsJsonArray()) {
                    TrackerHandler.trackerObjects.add(new TrackerObject(drop.getAsJsonObject(), locations));
                }
            }
            if (tracker.has("mobs")) {
                for (JsonElement mob : tracker.get("mobs").getAsJsonArray()) {
                    TrackerHandler.trackerObjects.add(new TrackerObject(mob.getAsJsonObject(), locations));
                }
            }
        }

        for (TrackerObject trackerObject : TrackerHandler.trackerObjects) {
            for (Locations location : trackerObject.getLocations()) {
                if (TrackerHandler.trackers.containsKey(location)) {
                    TrackerHandler.trackers.get(location).put(trackerObject.getInternalId(), trackerObject);
                } else {
                    HashMap<String, TrackerObject> value = new HashMap<>();
                    value.put(trackerObject.getInternalId(), trackerObject);
                    TrackerHandler.trackers.put(location, value);
                }
            }
        }
    }

    public static void loadTrackersFile() {
        TrackerHandler.trackers.clear();
        TrackerHandler.trackerObjects.clear();
        try {
            ResourceLocation trackers = new ResourceLocation("skyblockhud:data/trackers.json");
            InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(trackers).getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                loadTrackers(gson.fromJson(reader, JsonObject.class));
            }
        } catch (Exception ignored) {}
    }

    private static JsonElement getTrackerFile() {
        JsonArray stats = new JsonArray();
        TrackerHandler.trackerObjects.forEach(trackerObject -> {
            if (trackerObject.getCount() > 0) {
                JsonObject jsonObject = new JsonObject();
                JsonArray locations = new JsonArray();
                trackerObject.getLocations().forEach(l -> locations.add(new JsonPrimitive(l.toString().toUpperCase(Locale.ENGLISH))));
                jsonObject.add("id", new JsonPrimitive(trackerObject.getInternalId()));
                jsonObject.add("locations", locations);
                jsonObject.add("count", new JsonPrimitive(trackerObject.getCount()));
                stats.add(jsonObject);
            }
        });
        return stats;
    }

    public static boolean loadTrackerStatsFile() {
        File configFile = new File(LorenzMod.configDirectory, "sbh-trackers-stats.json");

        try {
            if (configFile.createNewFile()) {
                return true;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                if (json.has("trackerStats")) {
                    json
                        .getAsJsonArray("trackerStats")
                        .forEach(element -> {
                            if (element.isJsonObject()) {
                                JsonObject object = element.getAsJsonObject();
                                JsonArray locations = object.get("locations").getAsJsonArray();
                                Locations firstLocation = null;
                                for (JsonElement location : locations) {
                                    firstLocation = Locations.get(location.getAsString());
                                    if (!firstLocation.equals(Locations.DEFAULT)) break;
                                }

                                if (firstLocation != null && !firstLocation.equals(Locations.DEFAULT)) {
                                    TrackerHandler.trackers.get(firstLocation).get(object.get("id").getAsString()).setCount(object.get("count").getAsInt());
                                }
                            }
                        });

                    TrackerHandler.trackers.forEach((location, map) -> {
                        TrackerHandler.trackers.put(location, TrackerHandler.sortTrackers(map, (entry1, entry2) -> Integer.compare(entry2.getValue().getCount(), entry1.getValue().getCount())));
                    });
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void saveTrackerStatsFile() {
        File configFile = new File(LorenzMod.configDirectory, "sbh-trackers-stats.json");

        try {
            configFile.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                JsonObject json = new JsonObject();
                json.add("trackerStats", getTrackerFile());
                writer.write(gson.toJson(json));
            }
        } catch (IOException ignored) {}
    }
}
