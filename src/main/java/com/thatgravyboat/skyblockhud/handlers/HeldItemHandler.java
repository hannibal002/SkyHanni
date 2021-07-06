package com.thatgravyboat.skyblockhud.handlers;

import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.overlay.GenericOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HeldItemHandler extends Gui {

    public void drawFuelBar(Minecraft mc, int current, int max) {
        GenericOverlays.drawSmallBar(mc, 100, 100, (double) current / (double) max, 1.0d, 0xff00ff, 0xffff00, 0);
        drawString(mc.fontRendererObj, "Fuel - " + Math.round(((double) current / (double) max) * 100) + "%", 100, 100, 0xffffff);
    }

    public boolean isDrill(ItemStack stack) {
        if (stack == null) return false;
        if (!stack.getTagCompound().hasKey("ExtraAttributes")) return false;
        return stack.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("drill_fuel");
    }

    public String getDrillFuel(ItemStack stack) {
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int i = lore.tagCount() - 1; i >= 0; i--) {
            String line = Utils.removeColor(lore.getStringTagAt(i));
            if (line.trim().startsWith("Fuel:")) {
                return line;
            }
        }
        return "";
    }

    @SubscribeEvent
    public void drawOverlay(RenderGameOverlayEvent.Post event) {
        /*
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard())){
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack stack = mc.thePlayer.getHeldItem();
            if (isDrill(stack)){
                try {
                    String drill = getDrillFuel(stack).replace("Fuel:", "").trim();
                    String[] fuel = drill.split("/");
                    if (fuel.length == 2) {
                        int current = Integer.parseInt(fuel[0].replace(",", ""));
                        int max = Integer.parseInt(fuel[1].replace("k", "")) * 1000;
                        drawFuelBar(mc, current, max);
                    }
                }catch (Exception ignored){}
            }
        }
        */
    }
}
