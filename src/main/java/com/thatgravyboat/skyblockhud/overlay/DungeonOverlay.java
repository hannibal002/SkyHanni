package com.thatgravyboat.skyblockhud.overlay;

import com.thatgravyboat.skyblockhud.GuiTextures;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.SpecialColour;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.config.SBHConfig;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.dungeons.Classes;
import com.thatgravyboat.skyblockhud.dungeons.DungeonHandler;
import com.thatgravyboat.skyblockhud.dungeons.DungeonPlayer;
import com.thatgravyboat.skyblockhud.handlers.BossbarHandler;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.location.Locations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DungeonOverlay extends Gui {

    private static final FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
    private static boolean bossBarVisible = false;

    public void drawDungeonPlayer(String name, int health, boolean isDead, Classes dungeonClass, int x, int y) {
        if (!SkyblockHud.config.dungeon.hideDeadDungeonPlayers || !isDead) {
            GlStateManager.enableBlend();
            Minecraft mc = Minecraft.getMinecraft();
            mc.renderEngine.bindTexture(GuiTextures.dungeon);

            String healthString = isDead ? "DEAD" : Integer.toString(health);
            GlStateManager.color(1.0F, 1.0F, 1.0F, (float) SkyblockHud.config.dungeon.dungeonPlayerOpacity / 100);
            drawTexturedModalRect(x, y, 0, dungeonClass.getTextureY(), 120, 32);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawString(font, name, x + 50, y + 6, 0xFFFFFF);
            drawString(font, healthString, x + 50, y + font.FONT_HEIGHT + 9, 0xFF2B2B);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void drawDungeonClock(int width, int offset, Minecraft mc) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(GuiTextures.overlay);
        int dungeonTime = DungeonHandler.getDungeonTime();
        int dungeonTimeMin = dungeonTime / 60;
        int dungeonTimeSec = dungeonTime - dungeonTimeMin * 60;
        drawTexturedModalRect((width / 2) - 17, offset + (bossBarVisible ? 17 : 0), 0, 0, 34, 34);
        mc.renderEngine.bindTexture(GuiTextures.dungeon);
        drawTexturedModalRect((width / 2) - 7, offset + (bossBarVisible ? 20 : 3), 16, 50, 3, 8);
        drawTexturedModalRect((width / 2) - 7, offset + (bossBarVisible ? 30 : 13), 19, 50, 3, 8);
        String dungeonTimeElapsed = (dungeonTimeMin > 9 ? String.valueOf(dungeonTimeMin) : "0" + dungeonTimeMin) + ":" + (dungeonTimeSec > 9 ? String.valueOf(dungeonTimeSec) : "0" + dungeonTimeSec);
        drawCenteredString(font, dungeonTimeElapsed, (width / 2), offset + (bossBarVisible ? 40 : 23), 0xFFFF55);
        //KEYS
        drawString(font, (DungeonHandler.hasBloodkey() ? "\u2714" : "x"), (width / 2), offset + (bossBarVisible ? 19 : 2), (DungeonHandler.hasBloodkey() ? 0x55FF55 : 0xAA0000));
        drawString(font, DungeonHandler.getWitherKeys() + "x", (width / 2), offset + (bossBarVisible ? 30 : 13), 0x555555);
        //CLEARED PERCENTAGE
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(GuiTextures.overlay);
        int clearPercent = DungeonHandler.getDungeonCleared();
        String clearPercentage = "Dungeon Cleared: \u00A7" + (clearPercent <= 20 ? "4" : clearPercent <= 50 ? "6" : clearPercent <= 80 ? "e" : "a") + clearPercent + "%";
        drawTexturedModalRect((width / 2) + 17, offset + (bossBarVisible ? 20 : 3), 2, 34, font.getStringWidth(clearPercentage) + 3, 14);
        drawTexturedModalRect(((width / 2) + 17) + font.getStringWidth(clearPercentage) + 3, offset + (bossBarVisible ? 20 : 3), 252, 34, 4, 14);
        drawString(font, clearPercentage, (width / 2) + 18, offset + (bossBarVisible ? 23 : 6), 0xAAAAAA);

        //DEATHS
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(GuiTextures.overlay);
        int deaths = DungeonHandler.getDeaths();
        String deathText = "Deaths: " + deaths;
        drawTexturedModalRect((width / 2) + 17, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(deathText) + 3, 14);
        drawTexturedModalRect(((width / 2) + 17) + font.getStringWidth(deathText) + 3, offset + (bossBarVisible ? 35 : 18), 252, 34, 4, 14);
        drawString(font, deathText, (width / 2) + 18, offset + (bossBarVisible ? 38 : 21), 0xAAAAAA);

        //SECRETS
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(GuiTextures.overlay);
        int maxSecrets = DungeonHandler.getMaxSecrets();
        int secrets = DungeonHandler.getSecrets();
        int totalSecrets = DungeonHandler.getTotalSecrets();
        String secretsText = "Secrets: " + secrets + "/" + maxSecrets + " (" + totalSecrets + ")";
        drawTexturedModalRect((width / 2) - 17 - (font.getStringWidth(secretsText)) - 4, offset + (bossBarVisible ? 20 : 3), 0, 34, 2, 14);
        drawTexturedModalRect(((width / 2) - 17 - (font.getStringWidth(secretsText))) - 2, offset + (bossBarVisible ? 20 : 3), 2, 34, font.getStringWidth(secretsText) + 2, 14);
        drawString(font, secretsText, (width / 2) - 17 - (font.getStringWidth(secretsText)), offset + (bossBarVisible ? 23 : 6), 0xAAAAAA);

        //CRYPTS
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(GuiTextures.overlay);
        int crypts = DungeonHandler.getCrypts();
        String cryptText = "Crypts: " + crypts;
        drawTexturedModalRect((width / 2) - 17 - (font.getStringWidth(cryptText)) - 4, offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
        drawTexturedModalRect(((width / 2) - 17 - (font.getStringWidth(cryptText))) - 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(cryptText) + 2, 14);
        drawString(font, cryptText, (width / 2) - 17 - (font.getStringWidth(cryptText)), offset + (bossBarVisible ? 38 : 21), 0xAAAAAA);
    }

    public void drawUltimateBar(Minecraft mc, ScaledResolution resolution) {
        if (!SkyblockHud.config.dungeon.hideUltimateBar) {
            float percentage = mc.thePlayer.experience;
            SBHConfig.DungeonHud dungeonHud = SkyblockHud.config.dungeon;
            Position position = dungeonHud.barPosition;

            int x = position.getAbsX(resolution, 182);
            int y = position.getAbsY(resolution, 5);

            GenericOverlays.drawLargeBar(mc, x - 91, y, percentage, 0.999f, SpecialColour.specialToChromaRGB(dungeonHud.barLoadColor), SpecialColour.specialToChromaRGB(dungeonHud.barFullColor), dungeonHud.barStyle);
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS))) {
            bossBarVisible = BossStatus.statusBarTime > 0 && GuiIngameForge.renderBossHealth && BossbarHandler.bossBarRendered;
            GlStateManager.enableBlend();
            drawUltimateBar(mc, event.resolution);

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (!SkyblockHud.config.dungeon.hideDungeonPlayers) {
                int[] hardCodedPos = new int[] { 5, 42, 79, 116 };
                Position[] positions = new Position[] { SkyblockHud.config.dungeon.dungeonPlayer1, SkyblockHud.config.dungeon.dungeonPlayer2, SkyblockHud.config.dungeon.dungeonPlayer3, SkyblockHud.config.dungeon.dungeonPlayer4 };
                for (int i = 0; i < Math.min(DungeonHandler.getDungeonPlayers().values().size(), 4); i++) {
                    DungeonPlayer player = (DungeonPlayer) DungeonHandler.getDungeonPlayers().values().toArray()[i];
                    int posX;
                    int posY;
                    try {
                        posX = positions[i].getAbsX(event.resolution, 120);
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                        posX = hardCodedPos[i];
                    }
                    try {
                        posY = positions[i].getAbsY(event.resolution, 120);
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                        posY = 0;
                    }
                    drawDungeonPlayer(player.getName(), player.getHealth(), player.isDead(), player.getDungeonClass(), posX, posY);
                }
            }
            drawDungeonClock(event.resolution.getScaledWidth(), SkyblockHud.config.main.mainHudPos.getAbsY(event.resolution, 34), mc);
        }
    }
}
