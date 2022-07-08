package com.thatgravyboat.skyblockhud.overlay;

import at.lorenz.mod.LorenzMod;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.textures.Textures;
import com.thatgravyboat.skyblockhud.utils.Utils;
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

    public static int getHeat() {
        return heat;
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, LorenzMod.hasSkyblockScoreboard(), LorenzMod.config.mining.showDrillBar || LorenzMod.config.mining.showHeatBar)) {
            Minecraft mc = Minecraft.getMinecraft();

            if (LorenzMod.config.mining.barMode == 1) {
                if (heat > 0 && Utils.isDrill(mc.thePlayer.getHeldItem()) && LorenzMod.config.mining.showDrillBar && LorenzMod.config.mining.showHeatBar) {
                    renderFuelBar(mc, (event.resolution.getScaledWidth() / 2) - 91, event.resolution.getScaledHeight() - 31);
                    renderHeatBar(mc, (event.resolution.getScaledWidth() / 2) + 46, event.resolution.getScaledHeight() - 31);
                } else if (Utils.isDrill(mc.thePlayer.getHeldItem()) && LorenzMod.config.mining.showDrillBar) {
                    renderFuelBar(mc, (event.resolution.getScaledWidth() / 2) - 68, event.resolution.getScaledHeight() - 31);
                } else if (heat > 0 && LorenzMod.config.mining.showHeatBar) {
                    renderHeatBar(mc, (event.resolution.getScaledWidth() / 2) - 22, event.resolution.getScaledHeight() - 31);
                }
            } else if (LorenzMod.config.mining.barMode == 0) {
                if (heat > 0 && LorenzMod.config.mining.showHeatBar) {
                    Position position = LorenzMod.config.mining.heatBar;
                    renderHeatBar(mc, position.getAbsX(event.resolution, 45), position.getAbsY(event.resolution, 7));
                }
                if (Utils.isDrill(mc.thePlayer.getHeldItem()) && LorenzMod.config.mining.showDrillBar) {
                    Position position = LorenzMod.config.mining.drillBar;
                    renderFuelBar(mc, position.getAbsX(event.resolution, 136), position.getAbsY(event.resolution, 7));
                }
            }
        }
    }

    private void renderFuelBar(Minecraft mc, int x, int y) {
        if (maxFuel == 0) return;
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(Textures.texture.mines);
        drawTexturedModalRect(x, y, 0, 0, 136, 7);
        drawTexturedModalRect(x, y, 0, 7, Utils.lerp((float) fuel / (float) maxFuel, 0, 136), 7);
        String percentageText = Math.round(((float) fuel / (float) maxFuel) * 100) + "%";
        this.drawCenteredString(mc.fontRendererObj, percentageText, x + 68, y - 2, 0xffffff);
    }

    private void renderHeatBar(Minecraft mc, int x, int y) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(Textures.texture.mines);
        drawTexturedModalRect(x, y, 137, 0, 45, 7);
        drawTexturedModalRect(x, y, 137, 7, Utils.lerp(heat / 100f, 0, 45), 7);
    }
}
