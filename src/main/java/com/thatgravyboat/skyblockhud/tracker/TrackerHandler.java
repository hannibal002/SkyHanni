package com.thatgravyboat.skyblockhud.tracker;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import com.thatgravyboat.skyblockhud.seasons.SeasonDateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public class TrackerHandler {

    public static class TrackerData {
        public Map<String, Map<String,ItemStack>> dropTrackers;

        public TrackerData(Map<String, Map<String,ItemStack>> trackers) {
            this.dropTrackers = trackers;
        }

        public String getDropId(String event){
            if (event == null || event.isEmpty() || !eventGoing() || !dropTrackers.containsKey(event.toLowerCase().trim())) return null;
            return event.toLowerCase().trim();
        }

        private boolean eventGoing(){
            return SeasonDateHandler.getCurrentEventTime().trim().toLowerCase().contains("ends in");
        }
    }

    public static Map<String, TrackerData> trackers = new HashMap<>();
    public static Map<Locations,String> trackerIds = new HashMap<>();

    public static Map<String, ItemStack> sortTrackers(Map<String, ItemStack> map) {
        List<Map.Entry<String, ItemStack>> list = new ArrayList<>(map.entrySet());
        list.sort((entry1, entry2) -> Integer.compare(entry2.getValue().stackSize, entry1.getValue().stackSize));

        Map<String, ItemStack> result = new LinkedHashMap<>();
        for (Map.Entry<String, ItemStack> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static void onItemAdded(String id, int amount, String enchant, int level){
        if (SkyblockHud.hasSkyblockScoreboard() && trackerIds.containsKey(LocationHandler.getCurrentLocation())){
            String trackerId = trackerIds.get(LocationHandler.getCurrentLocation());
            TrackerData tracked = trackers.get(trackerId);
            String dropTrackerId = tracked.getDropId(SeasonDateHandler.getCurrentEvent());
            Map<String,ItemStack> tracker = tracked.dropTrackers.get(dropTrackerId);
            String dropId = id;
            if (enchant != null){
                dropId = enchant.toUpperCase() + ";" + level;
            }

            if (tracker != null && tracker.containsKey(dropId)){
                ItemStack stack = tracker.get(dropId);
                stack.stackSize += amount;
                tracked.dropTrackers.put(dropTrackerId, sortTrackers(tracker));
            }
        }
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if(stack == null)return;
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145;
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), trackerIds.containsKey(LocationHandler.getCurrentLocation()), !SkyblockHud.config.trackers.hideTracker)) {
            String trackerId = trackerIds.get(LocationHandler.getCurrentLocation());
            Minecraft mc = Minecraft.getMinecraft();
            TrackerData tracked = trackers.get(trackerId);

            Map<String, ItemStack> tracker = tracked.dropTrackers.get(tracked.getDropId(SeasonDateHandler.getCurrentEvent()));
            if (tracker != null) {
                Position pos = SkyblockHud.config.trackers.trackerPosition;
                int startPos = pos.getAbsX(event.resolution, (tracker.size() >= 6 ? 120 : tracker.size() * 20));
                int y = pos.getAbsY(event.resolution, (int) (10 + Math.ceil(tracker.size() / 5d) * 20));

                Gui.drawRect(startPos, y, startPos + 120, y + 10, -1072689136);
                mc.fontRendererObj.drawString("Tracker", startPos + 4, y + 1, 0xffffff, false);
                y += 10;
                Gui.drawRect(startPos, y, startPos + (tracker.size() >= 6 ? 120 : tracker.size() * 20), (int) (y + (Math.ceil(tracker.size() / 5d) * 20)), 1610612736);
                int x = startPos;
                for (ItemStack stack : tracker.values()) {
                    String s = String.valueOf(stack.stackSize);
                    GlStateManager.disableLighting();
                    GlStateManager.enableDepth();
                    drawItemStack(stack, x, y);
                    GlStateManager.disableDepth();
                    GlStateManager.disableBlend();
                    mc.fontRendererObj.drawStringWithShadow(s, (float) (x + 19 - 2 - mc.fontRendererObj.getStringWidth(s)), (float) (y + 9), stack.stackSize < 1 ? 16733525 : 16777215);
                    GlStateManager.enableBlend();
                    GlStateManager.enableDepth();

                    if ((x - startPos) / 20 == 5) {
                        x = startPos;
                        y += 20;
                    } else {
                        x += 20;
                    }
                }
            }
        }
    }


}
