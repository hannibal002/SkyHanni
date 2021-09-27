package com.thatgravyboat.skyblockhud.handlers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.*;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.api.events.ProfileJoinedEvent;
import com.thatgravyboat.skyblockhud.api.events.ProfileSwitchedEvent;
import com.thatgravyboat.skyblockhud.mixins.GuiChestAccessor;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WarpHandler {

    private static String profile = null;
    private static File warpConfig;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private static final SetMultimap<String, Warp> PLAYER_WARPS = HashMultimap.create();

    public static Collection<Warp> getWarps() {
        return PLAYER_WARPS.get(profile);
    }

    @SubscribeEvent
    public void profileChange(ProfileSwitchedEvent event) {
        if (profile != null && !profile.equals(event.profile)) {
            load();
        }
        profile = event.profile;
    }

    @SubscribeEvent
    public void profileJoined(ProfileJoinedEvent event) {
        if (profile != null && !profile.equals(event.profile)) {
            load();
        }
        profile = event.profile;
    }

    @SubscribeEvent
    public void onGuiClosed(GuiOpenEvent event) {
        boolean changed = false;
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiChest) {
            GuiChestAccessor accessor = (GuiChestAccessor) currentScreen;
            if (accessor.getLowerChestInventory().getDisplayName().getUnformattedText().contains("Fast Travel")) {
                for (int i = 9; i < Math.min(36, accessor.getLowerChestInventory().getSizeInventory()); i++) {
                    ItemStack stack = accessor.getLowerChestInventory().getStackInSlot(i);
                    if (stack != null && stack.getItem().equals(Items.skull) && stack.getTagCompound().hasKey("display")) {
                        NBTTagList lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);

                        String warpLine = Utils.removeColor(lore.getStringTagAt(0)).trim();

                        if (warpLine.equals("Unknown island!")) continue;

                        String disabledLine = Utils.removeColor(lore.getStringTagAt(lore.tagCount() - 1)).trim();

                        Warp warp = Warp.fromId(warpLine.replace("/warp", "").trim());

                        if (warp != null && !disabledLine.equals("Warp not unlocked!")) {
                            if (PLAYER_WARPS.put(profile, warp)) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        if (changed) save();
    }

    public static void save() {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        PLAYER_WARPS
            .asMap()
            .forEach(
                (profile, warps) -> {
                    JsonObject profileObject = new JsonObject();
                    profileObject.addProperty("profile", profile);
                    JsonArray warpArray = new JsonArray();
                    warps.forEach(warp -> warpArray.add(new JsonPrimitive(warp.name())));
                    profileObject.add("warps", warpArray);
                    array.add(profileObject);
                }
            );
        json.add("profileWarps", array);

        warpConfig = new File(SkyblockHud.configDirectory, "sbh-warps.json");

        try {
            warpConfig.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(warpConfig), StandardCharsets.UTF_8))) {
                writer.write(GSON.toJson(json));
            }
        } catch (IOException ignored) {}
    }

    public static boolean load() {
        warpConfig = new File(SkyblockHud.configDirectory, "sbh-warps.json");

        try {
            if (warpConfig.createNewFile()) return true;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(warpConfig), StandardCharsets.UTF_8))) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                json
                    .get("profileWarps")
                    .getAsJsonArray()
                    .forEach(
                        jsonElement -> {
                            JsonObject profileObject = jsonElement.getAsJsonObject();
                            List<Warp> warps = new ArrayList<>();
                            profileObject
                                .get("warps")
                                .getAsJsonArray()
                                .forEach(
                                    warpId -> {
                                        Warp warp = Warp.safeValueOf(warpId.getAsString());
                                        if (warp != null) warps.add(warp);
                                    }
                                );
                            PLAYER_WARPS.putAll(profileObject.get("profile").getAsString(), warps);
                        }
                    );
            }
        } catch (Exception ignored) {}
        return false;
    }

    public enum Warp {
        HUB("hub"),
        PRIVATE("home"),
        SPIDERSDEN("spider"),
        BLAZINGFORTRESS("nether"),
        THEEND("end"),
        THEPARK("park"),
        GOLDMINE("gold"),
        DEEPCAVERNS("deep"),
        DWARVENMINES("mines"),
        THEBARN("barn"),
        MUSHROOMDESERT("desert"),
        THECASTLE("castle"),
        SIRIUSSHACK("da"),
        GRAVEYARDCAVES("crypt"),
        SPIDERSNEST("nest"),
        MAGMACUBE("magma"),
        DRAGONNEST("drag"),
        JUNGLE("jungle"),
        HOWLINGCAVE("howl"),
        DUNGEONHUB("dungeon_hub");

        public String warpId;

        Warp(String warpId) {
            this.warpId = warpId;
        }

        public static Warp fromId(String id) {
            for (Warp value : Warp.values()) {
                if (value.warpId.equals(id)) return value;
            }
            return null;
        }

        public static Warp safeValueOf(String value) {
            try {
                return Warp.valueOf(value);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
