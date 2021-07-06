package com.thatgravyboat.skyblockhud.tracker;

import com.google.gson.*;
import com.thatgravyboat.skyblockhud.location.Locations;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerFileLoader {

    private static final Gson gson = new GsonBuilder().create();

    public static ItemStack getDisplayItem(JsonObject jsonObject){
        int meta = jsonObject.get("meta").getAsInt();
        String displayItemId = jsonObject.get("item").getAsString();
        Item item = Item.itemRegistry.getObject(new ResourceLocation(displayItemId));
        ItemStack stack = new ItemStack(item, 0, meta);
        if (jsonObject.has("skullData") && displayItemId.equals("minecraft:skull") && meta == 3){
            stack.setTagInfo("SkullOwner",getSkullTag(jsonObject.getAsJsonObject("skullData")));
        }
        if (jsonObject.has("enchanted") && jsonObject.get("enchanted").getAsBoolean()) stack.setTagInfo("ench", new NBTTagList());
        return stack;
    }

    public static NBTBase getSkullTag(JsonObject skullObject){
        NBTTagCompound skullOwner = new NBTTagCompound();
        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();
        NBTTagCompound value = new NBTTagCompound();

        skullOwner.setString("Id", skullObject.get("id").getAsString());

        value.setString("Value", skullObject.get("texture").getAsString());
        textures.appendTag(value);

        properties.setTag("textures",textures);

        skullOwner.setTag("Properties", properties);
        return skullOwner;
    }

    private static void loadTrackers(JsonObject object){
        for (JsonElement element : object.get("trackers").getAsJsonArray()) {
            JsonObject tracker = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            tracker.get("location").getAsJsonArray().forEach(loc -> builder.append(loc.getAsString()));
            String location = builder.toString();


            Map<String, ItemStack> stacks = new HashMap<>();
            for (JsonElement drop :tracker.get("drops").getAsJsonArray()) {
                JsonObject dropObject = drop.getAsJsonObject();

                //Display Item Creation
                ItemStack stack = getDisplayItem(dropObject.getAsJsonObject("displayItem"));
                String itemId = dropObject.get("id").getAsString();

                stacks.put(itemId, stack);
            }

            String event = tracker.has("event") ? tracker.get("event").getAsString() : null;

            Map<String, Map<String, ItemStack>> events = new HashMap<>();
            events.put(event, stacks);

            if (TrackerHandler.trackers.containsKey(location)){
                TrackerHandler.trackers.get(location).dropTrackers.put(event, stacks);
            }else {
                TrackerHandler.trackers.putIfAbsent(location, new TrackerHandler.TrackerData(events));
            }

            tracker.get("location").getAsJsonArray().forEach(loc -> TrackerHandler.trackerIds.put(Locations.get(loc.getAsString()), location));
        }
    }

    private static JsonElement getTrackerFile(){
        List<JsonObject> trackerStats = new ArrayList<>();
        TrackerHandler.trackers.forEach((locations, trackerData) ->
            trackerData.dropTrackers.forEach((event, drops) -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("location", locations);

                if (event == null) jsonObject.add("event", new JsonNull());
                else jsonObject.addProperty("event", event);

                JsonObject dropsData = new JsonObject();
                drops.forEach((s, stack) -> dropsData.addProperty(s, stack.stackSize));
                jsonObject.add("drops", dropsData);
                trackerStats.add(jsonObject);
            }
        ));
        JsonArray stats = new JsonArray();
        trackerStats.forEach(stats::add);
        return stats;
    }

    public static void loadTrackersFile(){
        try {
            ResourceLocation trackers = new ResourceLocation("skyblockhud:data/trackers.json");
            InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(trackers).getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                loadTrackers(gson.fromJson(reader, JsonObject.class));
            }
        }catch (Exception ignored){}
    }

    public static boolean loadTrackerStatsFile(File configDirectory){
        File configFile = new File(configDirectory, "sbh-trackers-stats.json");

        try {
            if (configFile.createNewFile()){
                return true;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                if (json.has("trackerStats")){
                    json.getAsJsonArray("trackerStats").forEach(element -> {
                        if (element.isJsonObject()){
                            JsonObject object = element.getAsJsonObject();
                            String location = object.get("location").getAsString();
                            Map<String, Map<String, ItemStack>> trackers = TrackerHandler.trackers.get(location).dropTrackers;

                            JsonElement event = object.get("event");
                            String eventString = event == null || event.isJsonNull() ? null : event.getAsString();
                            Map<String, ItemStack> drops = trackers.get(eventString);

                            if (drops != null) {
                                for (Map.Entry <String ,JsonElement> drop :object.getAsJsonObject("drops").entrySet()) {
                                    if (drops.containsKey(drop.getKey())) {
                                        drops.get(drop.getKey()).stackSize = drop.getValue().getAsInt();
                                    }
                                }
                                drops = TrackerHandler.sortTrackers(drops);
                                trackers.put(eventString, drops);
                            }
                        }
                    });
                }
            }
        } catch(Exception ignored) {}
        return false;
    }

    public static void saveTrackerStatsFile(File configDirectory){
        File configFile = new File(configDirectory, "sbh-trackers-stats.json");

        try {
            configFile.createNewFile();

            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                JsonObject json = new JsonObject();
                json.add("trackerStats", getTrackerFile());
                writer.write(gson.toJson(json));
            }
        } catch(IOException ignored) {}
    }

}
