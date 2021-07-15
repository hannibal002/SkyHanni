package com.thatgravyboat.skyblockhud.overlay;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.core.util.render.RenderUtils;
import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GenericOverlays extends Gui {

    public static int lastBar = 0;
    public static ResourceLocation bars = new ResourceLocation("skyblockhud", "bars.png");

    public static void drawLargeBar(Minecraft mc, int x, int y, float percentage, float max, int fullColor, int loadingColor, int barStyle) {
        if (SkyblockHud.hasSkyblockScoreboard()) {
            updateBar();
            mc.renderEngine.bindTexture(bars);
            Color color = new Color(percentage == max ? fullColor : loadingColor);

            RenderUtils.drawTexturedModalRect(x, y, 0, 0, 182, 5);
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            RenderUtils.drawTexturedModalRect(x, y, 0, 30, 182, 5);
            RenderUtils.drawTexturedModalRect(x, y, 0, 5, (int) (182 * percentage), 5);
            if (barStyle != 0) {
                RenderUtils.drawTexturedModalRect(x, y, 0, 5 + (barStyle * 5), 182, 5);
            }
        }
    }

    public static void drawSmallBar(Minecraft mc, int x, int y, double percentage, double max, int fullColor, int loadingColor, int barStyle) {
        if (SkyblockHud.hasSkyblockScoreboard()) {
            updateBar();
            mc.renderEngine.bindTexture(bars);
            Color color = new Color(percentage == max ? fullColor : loadingColor);
            GlStateManager.enableBlend();
            RenderUtils.drawTexturedModalRect(x, y, 0, 35, 62, 5);
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            RenderUtils.drawTexturedModalRect(x, y, 0, 65, 62, 5);
            RenderUtils.drawTexturedModalRect(x, y, 0, 40, (int) (62 * percentage), 5);
            if (barStyle != 0) {
                RenderUtils.drawTexturedModalRect(x, y, 0, 45 + (barStyle * 5), 62, 5);
            }
        }
    }

    public static void updateBar() {
        if (lastBar != SkyblockHud.config.misc.barTexture) {
            lastBar = SkyblockHud.config.misc.barTexture;
            if (lastBar == 0) bars = new ResourceLocation("skyblockhud", "bars.png"); else bars = new ResourceLocation("skyblockhud", "bars_" + lastBar + ".png");
        }
    }
}
