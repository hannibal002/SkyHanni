package com.thatgravyboat.skyblockhud.overlay;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.handlers.BossbarHandler;
import com.thatgravyboat.skyblockhud.handlers.CurrencyHandler;
import com.thatgravyboat.skyblockhud.handlers.SlayerHandler;
import com.thatgravyboat.skyblockhud.handlers.TimeHandler;
import com.thatgravyboat.skyblockhud.location.*;
import com.thatgravyboat.skyblockhud.seasons.Season;
import com.thatgravyboat.skyblockhud.seasons.SeasonDateHandler;
import com.thatgravyboat.skyblockhud.textures.Textures;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class OverlayHud extends Gui {

    private static final FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

    //STATS
    private static boolean eventToggle;

    public static boolean bossBarVisible = false;

    public void drawClock(int width, int offset, Minecraft mc) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        //CLOCK
        int timeMin = (int) (TimeHandler.time / 60);
        int timeHour = timeMin / 60;
        timeMin = timeMin - (timeHour * 60);
        String militaryTime = timeHour + ":" + (timeMin == 0 ? timeMin + "0" : timeMin);
        int time12Hour = timeHour >= 12 ? timeHour - 12 : timeHour;
        String normalTime = (time12Hour == 0 ? "12" : String.valueOf(time12Hour)) + ":" + (timeMin == 0 ? "00" : timeMin) + (timeHour >= 12 ? "pm" : "am");

        drawTexturedModalRect((width / 2) - 17, offset + (bossBarVisible ? 17 : 0), 0, 0, 34, 34);
        drawTexturedModalRect((width / 2) - 4, offset + (bossBarVisible ? 24 : 7), (timeHour > 19 || timeHour < 4) ? 43 : 43 + 8, 0, 8, 8);
        if (SkyblockHud.config.main.twelveHourClock) drawScaledString(0.8f, width / 2, offset + (bossBarVisible ? 38 : 21), normalTime, (timeHour > 19 || timeHour < 4) ? 0xAFB8CC : 0xFFFF55); else drawCenteredString(font, militaryTime, (width / 2), offset + (bossBarVisible ? 38 : 21), (timeHour > 19 || timeHour < 4) ? 0xAFB8CC : 0xFFFF55);

        //PURSE
        drawPurseAndBits(width, offset, mc);

        //SEASON/DATE
        drawSeasonAndDate(width, offset, mc);

        //REDSTONE PERCENT
        drawRedstone(width, offset, mc);

        // LOCATION
        drawLocation(width, offset, mc);

        //EXTRA SLOT
        if (LocationHandler.getCurrentLocation().equals(Locations.YOURISLAND)) {
            if (IslandHandler.flightTime > 0) drawFlightDuration(width, offset, mc);
        } else if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.MUSHROOMDESERT)) {
            drawTrapperOrPelts(width, offset, mc);
        } else if (LocationHandler.getCurrentLocation().getCategory().isMiningCategory()) {
            if (MinesHandler.currentEvent.display && LocationHandler.getCurrentLocation().getCategory() == LocationCategory.DWARVENMINES) {
                drawDwarvenEvent(width, offset, mc);
            } else {
                drawMiningPowders(width, offset, mc);
            }
        } else if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.PARK) && ParkIslandHandler.isRaining()) {
            if (LocationHandler.getCurrentLocation().equals(Locations.HOWLINGCAVE)) {
                drawSlayer(width, offset, mc);
            } else drawRainDuration(width, offset, mc);
        } else if (LocationHandler.getCurrentLocation().equals(Locations.FARMHOUSE)) {
            drawFarmHouseMedals(width, offset, mc);
        } else if (SlayerHandler.isDoingSlayer) {
            drawSlayer(width, offset, mc);
        }
    }

    public void drawSeasonAndDate(int width, int offset, Minecraft mc) {
        if (SeasonDateHandler.getCurrentSeason() != Season.ERROR) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (mc.thePlayer.ticksExisted % 100 == 0 && eventToggle) eventToggle = false;
            if (mc.thePlayer.ticksExisted % 600 == 0) eventToggle = true;
            mc.renderEngine.bindTexture(Textures.texture.stats);
            String dateText = SeasonDateHandler.getFancySeasonAndDate();
            if (eventToggle && !SeasonDateHandler.getCurrentEvent().isEmpty() && !SeasonDateHandler.getCurrentEventTime().isEmpty()) dateText = SeasonDateHandler.getCurrentEvent().trim() + " " + SeasonDateHandler.getCurrentEventTime().trim();
            drawTexturedModalRect((width / 2) + 17, offset + (bossBarVisible ? 20 : 3), 2, 34, font.getStringWidth(dateText) + 9, 14);
            drawTexturedModalRect(((width / 2) + 17) + font.getStringWidth(dateText) + 9, offset + (bossBarVisible ? 20 : 3), 252, 34, 4, 14);
            drawTexturedModalRect(((width / 2) + 17) + font.getStringWidth(dateText) + 2, offset + (bossBarVisible ? 23 : 6), SeasonDateHandler.getCurrentSeason().getTextureX(), 16, 8, 8);
            drawString(font, dateText, (width / 2) + 18, offset + (bossBarVisible ? 23 : 6), 0xffffff);
        }
    }

    public void drawLocation(int width, int offset, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(LocationHandler.getCurrentLocation().getDisplayName())), offset + (bossBarVisible ? 20 : 3), 0, 34, 2, 14);
        drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(LocationHandler.getCurrentLocation().getDisplayName()))) + 2, offset + (bossBarVisible ? 20 : 3), 2, 34, font.getStringWidth(LocationHandler.getCurrentLocation().getDisplayName()) + 14, 14);
        drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(LocationHandler.getCurrentLocation().getDisplayName()))) + 4, offset + (bossBarVisible ? 23 : 6), LocationHandler.getCurrentLocation().getCategory().getTexturePos(), 8, 8, 8);
        drawString(font, LocationHandler.getCurrentLocation().getDisplayName(), (width / 2) - 19 - (font.getStringWidth(LocationHandler.getCurrentLocation().getDisplayName())), offset + (bossBarVisible ? 23 : 6), 0xFFFFFF);
    }

    public void drawRedstone(int width, int offset, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        int redstoneColor = IslandHandler.redstone > 90 ? 0xFF0000 : IslandHandler.redstone > 75 ? 0xC45B00 : IslandHandler.redstone > 50 ? 0xFFFF55 : 0x55FF55;
        if (IslandHandler.redstone > 0 && Utils.isPlayerHoldingRedstone(mc.thePlayer)) {
            drawTexturedModalRect((width / 2) - 15, offset + (bossBarVisible ? 51 : 34), 0, 48, 30, 18);
            drawTexturedModalRect((width / 2) - 4, offset + (bossBarVisible ? 51 : 34), 59, 0, 8, 8);
            drawCenteredString(mc.fontRendererObj, IslandHandler.redstone + "%", (width / 2), offset + (bossBarVisible ? 58 : 41), redstoneColor);
        }
    }

    public void drawPurseAndBits(int width, int offset, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        int xPos = (width / 2) + 17;

        //COINS
        drawTexturedModalRect(xPos, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(CurrencyHandler.getCoinsFormatted()) + 11, 14);
        drawTexturedModalRect(xPos + 1, offset + (bossBarVisible ? 37 : 20), 34, 0, 8, 8);
        drawString(font, CurrencyHandler.getCoinsFormatted(), xPos + 10, offset + (bossBarVisible ? 38 : 21), 0xFFAA00);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        xPos += font.getStringWidth(CurrencyHandler.getCoinsFormatted()) + 11;

        //BITS
        if (CurrencyHandler.getBits() > 0) {
            drawTexturedModalRect(xPos, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(CurrencyHandler.getBitsFormatted()) + 11, 14);
            drawTexturedModalRect(xPos + 1, offset + (bossBarVisible ? 37 : 20), 75, 0, 8, 8);
            drawString(font, CurrencyHandler.getBitsFormatted(), xPos + 10, offset + (bossBarVisible ? 38 : 21), 0x55FFFF);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);
            xPos += font.getStringWidth(CurrencyHandler.getBitsFormatted()) + 11;
        }

        drawTexturedModalRect(xPos, offset + (bossBarVisible ? 35 : 18), 252, 34, 4, 14);
    }

    public void drawFlightDuration(int width, int offset, Minecraft mc) {
        if (LocationHandler.getCurrentLocation().equals(Locations.YOURISLAND)) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            DecimalFormat flightFormat = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.CANADA));
            String duration;
            if (IslandHandler.flightTime < 60) duration = IslandHandler.flightTime + "s"; else if (IslandHandler.flightTime < 3600) duration = flightFormat.format((double) IslandHandler.flightTime / 60) + "m"; else if (IslandHandler.flightTime < 86400) duration = flightFormat.format((double) IslandHandler.flightTime / 3600) + "hr"; else if (IslandHandler.flightTime < 86460) duration = flightFormat.format((double) IslandHandler.flightTime / 86400) + "day"; else duration = flightFormat.format((double) IslandHandler.flightTime / 86400) + "days";
            mc.renderEngine.bindTexture(Textures.texture.stats);
            drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(duration) + 14, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 4, offset + (bossBarVisible ? 38 : 21), 67, 0, 8, 8);
            drawString(font, duration, (width / 2) - 19 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
        }
    }

    public void drawRainDuration(int width, int offset, Minecraft mc) {
        if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.PARK)) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);
            String duration = "Rain: " + ParkIslandHandler.getRainTime();
            drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(duration) + 14, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 4, offset + (bossBarVisible ? 38 : 21), 83, 0, 8, 8);
            drawString(font, duration, (width / 2) - 19 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
        }
    }

    public void drawSlayer(int width, int offset, Minecraft mc) {
        if (SlayerHandler.isDoingSlayer) {
            int kills = SlayerHandler.progress;
            int maxKills = SlayerHandler.maxKills;
            int tier = SlayerHandler.slayerTier;
            SlayerHandler.slayerTypes slayerType = SlayerHandler.currentSlayer;
            if (slayerType != SlayerHandler.slayerTypes.NONE) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.renderEngine.bindTexture(Textures.texture.stats);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(EnumChatFormatting.GREEN);
                stringBuilder.append(Utils.intToRomanNumeral(tier));
                stringBuilder.append(" ");
                if (SlayerHandler.isKillingBoss) {
                    stringBuilder.append(EnumChatFormatting.RED);
                    stringBuilder.append("Slay Boss!");
                } else if (SlayerHandler.bossSlain) {
                    stringBuilder.append(EnumChatFormatting.RED);
                    stringBuilder.append("Boss Slain!");
                } else if (kills == 0 && maxKills == 0) {
                    stringBuilder.append(EnumChatFormatting.RED);
                    stringBuilder.append("Not Slaying!");
                } else {
                    stringBuilder.append(EnumChatFormatting.YELLOW);
                    stringBuilder.append(kills);
                    stringBuilder.append(EnumChatFormatting.GRAY);
                    stringBuilder.append("/");
                    stringBuilder.append(EnumChatFormatting.RED);
                    stringBuilder.append(maxKills);
                }
                String text = stringBuilder.toString();
                drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(text)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(text))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(text) + 14, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(text))) + 4, offset + (bossBarVisible ? 38 : 21), slayerType.getX(), 24, 8, 8);
                drawString(font, text, (width / 2) - 19 - (font.getStringWidth(text)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
            }
        }
    }

    public void drawMiningPowders(int width, int offset, Minecraft mc) {
        if (MinesHandler.gemstone == 0) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);
            String mithril = MinesHandler.getMithrilFormatted();
            drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(mithril)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(mithril))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(mithril) + 14, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(mithril))) + 4, offset + (bossBarVisible ? 38 : 21), 91, 0, 8, 8);
            drawString(font, mithril, (width / 2) - 19 - (font.getStringWidth(mithril)), offset + (bossBarVisible ? 38 : 21), 0x00C896);
        } else {
            LocationCategory locationCategory = LocationHandler.getCurrentLocation().getCategory();
            String mithril = locationCategory == LocationCategory.DWARVENMINES ? MinesHandler.getMithrilFormatted() : MinesHandler.getMithrilShortFormatted();
            String gemstone = locationCategory == LocationCategory.CRYSTALHOLLOWS ? MinesHandler.getGemstoneFormatted() : MinesHandler.getGemstoneShortFormatted();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);

            int edge = (width / 2) - 33;

            int barWidth = font.getStringWidth(mithril) + 12 + font.getStringWidth(gemstone);

            int firstText = locationCategory == LocationCategory.DWARVENMINES ? font.getStringWidth(mithril) : font.getStringWidth(gemstone);

            //Bar
            drawTexturedModalRect(edge - barWidth, offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
            drawTexturedModalRect(edge - barWidth + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, barWidth + 14, 14);

            //Icons
            drawTexturedModalRect(edge - barWidth + 4, offset + (bossBarVisible ? 38 : 21), locationCategory == LocationCategory.DWARVENMINES ? 91 : 131, 0, 8, 8);
            drawTexturedModalRect(edge - barWidth + 16 + firstText, offset + (bossBarVisible ? 38 : 21), locationCategory == LocationCategory.DWARVENMINES ? 131 : 91, 0, 8, 8);

            drawString(font, locationCategory == LocationCategory.DWARVENMINES ? mithril : gemstone, edge - barWidth + 14, offset + (bossBarVisible ? 38 : 21), locationCategory == LocationCategory.DWARVENMINES ? 0x00C896 : 0xFF55FF);
            drawString(font, locationCategory == LocationCategory.DWARVENMINES ? gemstone : mithril, edge - barWidth + 26 + firstText, offset + (bossBarVisible ? 38 : 21), locationCategory == LocationCategory.DWARVENMINES ? 0xFF55FF : 0x00C896);
        }
    }

    public void drawTrapperOrPelts(int width, int offset, Minecraft mc) {
        if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.MUSHROOMDESERT)) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);
            String duration = FarmingIslandHandler.location != Locations.NONE ? FarmingIslandHandler.location.getDisplayName() : "" + FarmingIslandHandler.pelts;
            drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(duration) + 14, 14);
            drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 4, offset + (bossBarVisible ? 38 : 21), FarmingIslandHandler.location != Locations.NONE ? 123 : 115, 0, 8, 8);
            drawString(font, duration, (width / 2) - 19 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
        }
    }

    public void drawDwarvenEvent(int width, int offset, Minecraft mc) {
        if (LocationHandler.getCurrentLocation().getCategory().equals(LocationCategory.DWARVENMINES)) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.renderEngine.bindTexture(Textures.texture.stats);
            if (MinesHandler.eventMax > 0 || !MinesHandler.currentEvent.needsMax) {
                String duration = MinesHandler.currentEvent.needsMax ? MinesHandler.eventProgress + "/" + MinesHandler.eventMax : String.valueOf(MinesHandler.eventProgress);
                drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(duration) + 14, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(duration))) + 4, offset + (bossBarVisible ? 38 : 21), MinesHandler.currentEvent.x, 0, 8, 8);
                drawString(font, duration, (width / 2) - 19 - (font.getStringWidth(duration)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
            } else {
                String text = MinesHandler.currentEvent.displayName;
                drawTexturedModalRect((width / 2) - 33 - (font.getStringWidth(text)), offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(text))) + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, font.getStringWidth(text) + 14, 14);
                drawTexturedModalRect(((width / 2) - 33 - (font.getStringWidth(text))) + 4, offset + (bossBarVisible ? 38 : 21), MinesHandler.currentEvent.x, 0, 8, 8);
                drawString(font, text, (width / 2) - 19 - (font.getStringWidth(text)), offset + (bossBarVisible ? 38 : 21), 0xFFFFFF);
            }
        }
    }

    public void drawFarmHouseMedals(int width, int offset, Minecraft mc) {
        if (LocationHandler.getCurrentLocation().equals(Locations.FARMHOUSE)) {
            int bronze = font.getStringWidth(FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.BRONZE));
            int silver = font.getStringWidth(FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.SILVER));
            int gold = font.getStringWidth(FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.GOLD));

            int end = drawLeftBottomBar(width, offset, 40 + bronze + silver + gold, mc);
            drawTexturedModalRect(end + 2, offset + (bossBarVisible ? 38 : 21), 139, 0, 8, 8);
            drawTexturedModalRect(end + 14 + gold, offset + (bossBarVisible ? 38 : 21), 147, 0, 8, 8);
            drawTexturedModalRect(end + 26 + gold + silver, offset + (bossBarVisible ? 38 : 21), 155, 0, 8, 8);

            drawString(font, FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.GOLD), end + 12, offset + (bossBarVisible ? 38 : 21), 0xffffff);
            drawString(font, FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.SILVER), end + gold + 24, offset + (bossBarVisible ? 38 : 21), 0xffffff);
            drawString(font, FarmHouseHandler.getFormattedMedals(FarmHouseHandler.Medal.BRONZE), end + gold + silver + 36, offset + (bossBarVisible ? 38 : 21), 0xffffff);
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard())) {
            bossBarVisible = BossStatus.statusBarTime > 0 && GuiIngameForge.renderBossHealth && BossbarHandler.bossBarRendered;
            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (LocationHandler.getCurrentLocation() != Locations.CATACOMBS) {
                drawClock(event.resolution.getScaledWidth(), SkyblockHud.config.main.mainHudPos.getAbsY(event.resolution, 34), mc);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void drawScaledString(float factor, int x, int y, String text, int color) {
        GlStateManager.scale(factor, factor, 1);
        drawCenteredString(font, text, (int) (x / factor), (int) (y / factor), color);
        GlStateManager.scale(1 / factor, 1 / factor, 1);
    }

    public int drawLeftBottomBar(int width, int offset, int barWidth, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Textures.texture.stats);
        int edge = (width / 2) - 17;

        drawTexturedModalRect(edge - barWidth, offset + (bossBarVisible ? 35 : 18), 0, 34, 2, 14);
        drawTexturedModalRect(edge - barWidth + 2, offset + (bossBarVisible ? 35 : 18), 2, 34, barWidth - 2, 14);
        return edge - barWidth + 2;
    }
}
