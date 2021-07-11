package com.thatgravyboat.skyblockhud.overlay;

import com.thatgravyboat.skyblockhud.GuiTextures;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MiningHud extends Gui {

    private static int fuel, maxFuel;
    private static int heat;

    public static void setFuel(int fuel, int maxFuel) {
        MiningHud.fuel = fuel;
        MiningHud.maxFuel = maxFuel;
    }

    public static void setHeat(int heat) {
        MiningHud.heat = heat;
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), SkyblockHud.config.miningHud.showDrillBar || SkyblockHud.config.miningHud.showHeatBar)) {
            Minecraft mc = Minecraft.getMinecraft();

            if (heat > 0 && Utils.isDrill(mc.thePlayer.getHeldItem()) && SkyblockHud.config.miningHud.showDrillBar && SkyblockHud.config.miningHud.showHeatBar) {
                renderFuelBar(mc, (event.resolution.getScaledWidth() / 2) - 91, event.resolution.getScaledHeight() - 31);
                renderHeatBar(mc, (event.resolution.getScaledWidth() / 2) + 46, event.resolution.getScaledHeight() - 31);
            } else if (Utils.isDrill(mc.thePlayer.getHeldItem()) && SkyblockHud.config.miningHud.showDrillBar) {
                renderFuelBar(mc, (event.resolution.getScaledWidth() / 2) - 68, event.resolution.getScaledHeight() - 31);
            } else if (heat > 0 && SkyblockHud.config.miningHud.showHeatBar) {
                renderHeatBar(mc, (event.resolution.getScaledWidth() / 2) - 22, event.resolution.getScaledHeight() - 31);
            }
        }
    }

    private void renderFuelBar(Minecraft mc, int x, int y) {
        if (maxFuel == 0) return;
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(GuiTextures.mining);
        drawTexturedModalRect(x, y, 0, 0, 136, 7);
        drawTexturedModalRect(x, y, 0, 7, Utils.lerp((float) fuel / (float) maxFuel, 0, 136), 7);
        String percentageText = Math.round(((float) fuel / (float) maxFuel) * 100) + "%";
        this.drawCenteredString(mc.fontRendererObj, percentageText, x + 68, y - 2, 0xffffff);
    }

    private void renderHeatBar(Minecraft mc, int x, int y) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(GuiTextures.mining);
        drawTexturedModalRect(x, y, 137, 0, 45, 7);
        drawTexturedModalRect(x, y, 137, 7, Utils.lerp(heat / 100f, 0, 45), 7);
    }
}
