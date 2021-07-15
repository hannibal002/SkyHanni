package com.thatgravyboat.skyblockhud.tracker;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.api.events.SkyBlockEntityKilled;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TrackerHandler extends Gui {

    public static Set<TrackerObject> trackerObjects = new HashSet<>();
    public static Map<Locations, Map<String, TrackerObject>> trackers = new HashMap<>();

    public static <K,V> Map<K, V> sortTrackers(Map<K, V> map, Comparator<? super Map.Entry<K, V>> comparator) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(comparator);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static void onItemAdded(String id, int amount, String specialId, int number) {
        if (SkyblockHud.hasSkyblockScoreboard() && trackers.containsKey(LocationHandler.getCurrentLocation())) {
            Map<String, TrackerObject> trackerMap = trackers.get(LocationHandler.getCurrentLocation());
            String dropId = id;
            if (specialId != null) {
                dropId = specialId.toUpperCase() + ";" + number;
            }

            if (trackerMap != null && trackerMap.containsKey(dropId)) {
                TrackerObject object = trackerMap.get(dropId);
                object.increaseCount(amount);
                trackers.put(LocationHandler.getCurrentLocation(), sortTrackers(trackerMap, (entry1, entry2) -> Integer.compare(entry2.getValue().getCount(), entry1.getValue().getCount())));
            }

        }
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if (stack == null) return;
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145;
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    @SubscribeEvent
    public void onSbEntityDeath(SkyBlockEntityKilled event){
        System.out.println(event.id);
        if (SkyblockHud.hasSkyblockScoreboard() && trackers.containsKey(LocationHandler.getCurrentLocation())) {
            Map<String, TrackerObject> trackerMap = trackers.get(LocationHandler.getCurrentLocation());
            if (trackerMap.containsKey("ENTITY:"+event.id)){
                TrackerObject object = trackerMap.get("ENTITY:"+event.id);
                object.increaseCount();
                trackers.put(LocationHandler.getCurrentLocation(), sortTrackers(trackerMap, (entry1, entry2) -> Integer.compare(entry2.getValue().getCount(), entry1.getValue().getCount())));
            }
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), trackers.containsKey(LocationHandler.getCurrentLocation()),!SkyblockHud.config.trackers.hideTracker)) {
            Map<String, TrackerObject> tracker = trackers.get(LocationHandler.getCurrentLocation());
            Minecraft mc = Minecraft.getMinecraft();

            if (tracker != null) {
                Position pos = SkyblockHud.config.trackers.trackerPosition;
                int startPos = pos.getAbsX(event.resolution, (tracker.size() >= 6 ? 130 : tracker.size() * 20));
                int y = pos.getAbsY(event.resolution, (int) (10 + Math.ceil(tracker.size() / 5d) * 20));

                Gui.drawRect(startPos, y, startPos + 130, y + 10, -1072689136);
                mc.fontRendererObj.drawString("Tracker", startPos + 4, y + 1, 0xffffff, false);
                y += 10;
                Gui.drawRect(startPos, y, startPos + (tracker.size() >= 6 ? 130 : (tracker.size() * 20)+10), (int) (y + (Math.ceil(tracker.size() / 5d) * 20)), 1610612736);
                int x = startPos + 5;
                for (TrackerObject object : tracker.values()) {
                    String s = Utils.formattedNumber(object.getCount(), 1000);
                    GlStateManager.disableLighting();
                    GlStateManager.enableDepth();
                    drawItemStack(object.getDisplayStack(), x, y);
                    GlStateManager.disableDepth();
                    GlStateManager.disableBlend();
                    mc.fontRendererObj.drawStringWithShadow(s, (float) (x + 19 - 2 - mc.fontRendererObj.getStringWidth(s)), (float) (y + 9), object.getCount() < 1 ? 16733525 : 16777215);
                    GlStateManager.enableBlend();
                    GlStateManager.enableDepth();

                    if ((x - startPos + 5) / 20 == 5) {
                        x = startPos + 5;
                        y += 20;
                    } else {
                        x += 20;
                    }
                }
            }
        }
    }
}
