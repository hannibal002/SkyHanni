package com.thatgravyboat.skyblockhud.handlers;

import static com.thatgravyboat.skyblockhud.GuiTextures.mapOverlay;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.config.KeyBindings;
import com.thatgravyboat.skyblockhud.config.SBHConfig;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.handlers.mapicons.DwarvenIcons;
import com.thatgravyboat.skyblockhud.handlers.mapicons.HubIcons;
import com.thatgravyboat.skyblockhud.location.LocationHandler;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.vecmath.Vector2f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class MapHandler {

    public enum MapIconTypes {
        SHOPS,
        MISC,
        NPC,
        INFO,
        QUEST
    }

    public static class MapIcon {

        public Vector2f position;
        public ResourceLocation icon;
        public String tooltip;
        public String command;
        public MapIconTypes type;

        public MapIcon(Vector2f pos, ResourceLocation icon, String tooltip, MapIconTypes type) {
            this(pos, icon, tooltip, type, "");
        }

        public MapIcon(Vector2f pos, ResourceLocation icon, String tooltip, MapIconTypes type, String command) {
            this.position = pos;
            this.icon = icon;
            this.tooltip = tooltip;
            this.type = type;
            this.command = command;
        }

        public boolean cantRender() {
            SBHConfig.Map mapConfig = SkyblockHud.config.map;
            if (mapConfig.showInfoIcons && type.equals(MapIconTypes.INFO)) return false; else if (mapConfig.showMiscIcons && type.equals(MapIconTypes.MISC)) return false; else if (mapConfig.showNpcIcons && type.equals(MapIconTypes.NPC)) return false; else if (mapConfig.showQuestIcons && type.equals(MapIconTypes.QUEST)) return false; else return (!mapConfig.showShopIcons || !type.equals(MapIconTypes.SHOPS));
        }
    }

    public enum Maps {
        HUB(0.5f, 494, 444, 294, 218, 294, 224, new ResourceLocation("skyblockhud", "maps/hub.png"), HubIcons.hubIcons),
        MUSHROOM(1.0f, 318, 316, -84, 605, -84, 612, new ResourceLocation("skyblockhud", "maps/mushroom.png"), Collections.emptyList()),
        SPIDERS(1.0f, 270, 238, 400, 362, 400, 364, new ResourceLocation("skyblockhud", "maps/spidersden.png"), Collections.emptyList()),
        NETHER(0.5f, 257, 371, 436, 732, 433, 736, new ResourceLocation("skyblockhud", "maps/fort.png"), Collections.emptyList()),
        BARN(1.5f, 135, 130, -82, 320, -81, 318, new ResourceLocation("skyblockhud", "maps/barn.png"), Collections.emptyList()),
        DWARVEN(0.5f, 409, 461, 206, 160, 202, 166, new ResourceLocation("skyblockhud", "maps/dwarven.png"), DwarvenIcons.dwarvenIcons),
        CRYSTAL(0.5f, 624, 624, -202, -215.7, -202, -212, new ResourceLocation("skyblockhud", "maps/crystal.png"), Collections.emptyList()),
        PARK(1f, 211, 230, 480, 133, 478, 134, new ResourceLocation("skyblockhud", "maps/park.png"), Collections.emptyList());

        public float scaleFactor;
        public int width;
        public int height;
        public double xMiniOffset;
        public double yMiniOffset;
        public double xOffset;
        public double yOffset;
        public ResourceLocation mapTexture;
        public List<MapIcon> icons;

        Maps(float scaleFactor, int width, int height, double xMiniOffset, double yMiniOffset, double xOffset, double yOffset, ResourceLocation mapTexture, List<MapIcon> icons) {
            this.scaleFactor = scaleFactor;
            this.width = width;
            this.height = height;
            this.xMiniOffset = xMiniOffset;
            this.yMiniOffset = yMiniOffset;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.mapTexture = mapTexture;
            this.icons = icons;
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), SkyblockHud.config.map.showMiniMap)) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof MapScreen) return;
            if (LocationHandler.getCurrentLocation().getCategory().getMap() == null) return;
            if (mc.thePlayer != null) {
                MapHandler.Maps map = LocationHandler.getCurrentLocation().getCategory().getMap();
                mc.renderEngine.bindTexture(mapOverlay);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                Position pos = SkyblockHud.config.map.miniMapPosition;
                Gui.drawModalRectWithCustomSizedTexture(pos.getAbsX(event.resolution, 72), pos.getAbsY(event.resolution, 72), 72, 0, 72, 72, 256, 256);
                mc.renderEngine.bindTexture(map.mapTexture);

                double x = mc.thePlayer.getPosition().getX() + map.xMiniOffset;
                double z = mc.thePlayer.getPosition().getZ() + map.yMiniOffset;
                float u = (float) ((x / (map.width / 256f)) - 33f);
                float v = (float) ((z / (map.height / 256f)) - 28f);

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

                Gui.drawModalRectWithCustomSizedTexture(pos.getAbsX(event.resolution, 72) + 4, pos.getAbsY(event.resolution, 72) + 2, u, v, 64, 64, 256, 256);

                if (SkyblockHud.config.map.showPlayerLocation) {
                    mc.fontRendererObj.drawString("\u2022", pos.getAbsX(event.resolution, 72) + 36, pos.getAbsY(event.resolution, 72) + 34, 0xff0000, false);
                }

                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                mc.renderEngine.bindTexture(mapOverlay);
                Gui.drawModalRectWithCustomSizedTexture(pos.getAbsX(event.resolution, 72), pos.getAbsY(event.resolution, 72), 0, 0, 72, 72, 256, 256);
                String keyCode = GameSettings.getKeyDisplayString(KeyBindings.map.getKeyCode());
                Utils.drawStringCenteredScaled(keyCode, mc.fontRendererObj, pos.getAbsX(event.resolution, 64) + (pos.rightAligned(event.resolution, 72) ? 50 : 58), pos.getAbsY(event.resolution, 72) + 66, false, 6, 0xFFFFFF);
                BlockPos playerPos = mc.thePlayer.getPosition();
                String position = String.format("%d/%d/%d", playerPos.getX(), playerPos.getY(), playerPos.getZ());
                Utils.drawStringCenteredScaled(position, mc.fontRendererObj, pos.getAbsX(event.resolution, 64) + (pos.rightAligned(event.resolution, 72) ? 21 : 29), pos.getAbsY(event.resolution, 72) + 66, false, 36, 0xFFFFFF);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (KeyBindings.map.isPressed() && LocationHandler.getCurrentLocation().getCategory().getMap() != null && SkyblockHud.hasSkyblockScoreboard()) SkyblockHud.screenToOpen = new MapScreen();
    }

    public static class MapScreen extends GuiScreen {

        public MapHandler.Maps map = LocationHandler.getCurrentLocation().getCategory().getMap();

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            this.drawWorldBackground(0);
            this.mc.renderEngine.bindTexture(map.mapTexture);
            float mapX = (width / 2f) - ((map.width / 2f) * map.scaleFactor);
            float mapY = (height / 2f) - ((map.height / 2f) * map.scaleFactor);
            Gui.drawModalRectWithCustomSizedTexture((int) mapX, (int) mapY, 0, 0, (int) (map.width * map.scaleFactor), (int) (map.height * map.scaleFactor), (int) (map.width * map.scaleFactor), (int) (map.height * map.scaleFactor));
            drawIcons((int) mapX, (int) mapY);
            if (this.mc.thePlayer != null && SkyblockHud.config.map.showPlayerLocation) {
                double x = this.mc.thePlayer.getPosition().getX() + map.xOffset;
                double z = this.mc.thePlayer.getPosition().getZ() + map.yOffset;
                fontRendererObj.drawString("\u2022", (int) (x * map.scaleFactor + mapX), (int) (z * map.scaleFactor + mapY), 0xff0000);
            }
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            onTooltip(mouseX, mouseY, (int) mapX, (int) mapY);
        }

        public void drawIcons(int startX, int startY) {
            if (map.icons == null) return;
            for (MapIcon icon : map.icons) {
                if (icon.cantRender()) continue;
                GlStateManager.enableBlend();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                this.mc.renderEngine.bindTexture(icon.icon);
                double x = ((icon.position.x + map.xOffset) * map.scaleFactor) + startX - 4;
                double y = ((icon.position.y + map.yOffset) * map.scaleFactor) + startY - 4;
                Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, 8, 8, 8, 8);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        public void onTooltip(int mouseX, int mouseY, int startX, int startY) {
            if (map.icons == null) return;
            for (MapIcon icon : map.icons) {
                if (icon.cantRender()) continue;
                if (Utils.inRangeInclusive(mouseX, (int) ((icon.position.x + map.xOffset) * map.scaleFactor) + startX - 4, (int) ((icon.position.x + map.xOffset) * map.scaleFactor) + startX + 4) && Utils.inRangeInclusive(mouseY, (int) ((icon.position.y + map.yOffset) * map.scaleFactor) + startY - 4, (int) ((icon.position.y + map.yOffset) * map.scaleFactor) + startY + 4)) {
                    drawHoveringText(Arrays.asList(icon.tooltip.split("\n")), mouseX, mouseY);
                    break;
                }
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            int mapX = (int) ((width / 2f) - ((map.width / 2f) * map.scaleFactor));
            int mapY = (int) ((height / 2f) - ((map.height / 2f) * map.scaleFactor));
            for (MapIcon icon : map.icons) {
                if (icon.cantRender()) continue;
                if (Utils.inRangeInclusive(mouseX, (int) ((icon.position.x + map.xOffset) * map.scaleFactor) + mapX - 4, (int) ((icon.position.x + map.xOffset) * map.scaleFactor) + mapX + 4) && Utils.inRangeInclusive(mouseY, (int) ((icon.position.y + map.yOffset) * map.scaleFactor) + mapY - 4, (int) ((icon.position.y + map.yOffset) * map.scaleFactor) + mapY + 4)) {
                    if (!icon.command.isEmpty()) {
                        this.mc.thePlayer.sendChatMessage("/" + icon.command);
                    }
                    break;
                }
            }
        }
    }
}
