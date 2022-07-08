package com.thatgravyboat.skyblockhud.utils;

import at.lorenz.mod.LorenzMod;
import java.math.RoundingMode;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class Utils {

    private static final LinkedList<Integer> guiScales = new LinkedList<>();
    private static ScaledResolution lastScale = new ScaledResolution(Minecraft.getMinecraft());
    //Labymod compatibility
    private static final FloatBuffer projectionMatrixOld = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer modelviewMatrixOld = BufferUtils.createFloatBuffer(16);

    public static String removeColor(String input) {
        return input.replaceAll("(?i)\\u00A7.", "");
    }

    public static String removeWhiteSpaceAndRemoveWord(String input, String replace) {
        return input.toLowerCase().replace(" ", "").replace(replace, "");
    }

//    public static boolean isPlayerHoldingRedstone(EntityPlayerSP player) {
//        if (!LorenzMod.config.main.requireRedstone) return true;
//        ArrayList<Item> redstoneItems = new ArrayList<>(Arrays.asList(Items.redstone, Items.repeater, Items.comparator, Item.getByNameOrId("minecraft:redstone_torch")));
//        if (player.getHeldItem() != null) return redstoneItems.contains(player.getHeldItem().getItem());
//        return false;
//    }

    public static boolean inRangeInclusive(int value, int min, int max) {
        return value <= max && value >= min;
    }

    public static float lerp(float f, float g, float h) {
        return g + f * (h - g);
    }

    public static double lerp(double d, double e, double f) {
        return e + d * (f - e);
    }

    public static int lerp(float f, int g, int h) {
        return (int) (g + f * (h - g));
    }

    public static NBTTagCompound getSkyBlockTag(ItemStack stack) {
        if (stack == null) return null;
        if (!stack.hasTagCompound()) return null;
        if (!stack.getTagCompound().hasKey("ExtraAttributes")) return null;
        return stack.getTagCompound().getCompoundTag("ExtraAttributes");
    }

    public static boolean isDrill(ItemStack stack) {
        NBTTagCompound tag = getSkyBlockTag(stack);
        return tag != null && tag.hasKey("drill_fuel");
    }

    public static int whatRomanNumeral(String roman) {
        switch (roman.toLowerCase()) {
            case "i":
                return 1;
            case "ii":
                return 2;
            case "iii":
                return 3;
            case "iv":
                return 4;
            case "v":
                return 5;
            case "vi":
                return 6;
            case "vii":
                return 7;
            case "viii":
                return 8;
            case "ix":
                return 9;
            case "x":
                return 10;
            default:
                return 0;
        }
    }

    public static String intToRomanNumeral(int i) {
        switch (i) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return "";
        }
    }

    public static boolean overlayShouldRender(RenderGameOverlayEvent.ElementType type, boolean... booleans) {
        return overlayShouldRender(false, type, RenderGameOverlayEvent.ElementType.HOTBAR, booleans);
    }

    public static boolean overlayShouldRender(boolean hideOnf3, RenderGameOverlayEvent.ElementType type, RenderGameOverlayEvent.ElementType checkType, boolean... booleans) {
        Minecraft mc = Minecraft.getMinecraft();
        for (boolean aBoolean : booleans) if (!aBoolean) return false;
        if (hideOnf3) {
            if (mc.gameSettings.showDebugInfo || (mc.gameSettings.keyBindPlayerList.isKeyDown() && (!mc.isIntegratedServerRunning() || mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1))) {
                return false;
            }
        }
        return ((type == null && Loader.isModLoaded("labymod")) || type == checkType);
    }

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len / (float) strLen;
        factor = Math.min(1, factor);

        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int colour, float factor) {
        GlStateManager.scale(factor, factor, 1);
        fr.drawString(str, x / factor, y / factor, colour, shadow);
        GlStateManager.scale(1 / factor, 1 / factor, 1);
    }

    public static void drawStringCenteredScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len / (float) strLen;
        float fontHeight = 8 * factor;

        drawStringScaled(str, fr, x - len / 2f, y - fontHeight / 2f, shadow, colour, factor);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(uMin, vMin).endVertex();
        tessellator.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManager.disableBlend();
    }

    public static void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1, filter);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
    }

    public static void resetGuiScale() {
        guiScales.clear();
    }

    public static ScaledResolution peekGuiScale() {
        return lastScale;
    }

    public static ScaledResolution pushGuiScale(int scale) {
        if (guiScales.size() == 0) {
            if (Loader.isModLoaded("labymod")) {
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixOld);
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewMatrixOld);
            }
        }

        if (scale < 0) {
            if (guiScales.size() > 0) {
                guiScales.pop();
            }
        } else {
            if (scale == 0) {
                guiScales.push(Minecraft.getMinecraft().gameSettings.guiScale);
            } else {
                guiScales.push(scale);
            }
        }

        int newScale = guiScales.size() > 0 ? Math.max(0, Math.min(4, guiScales.peek())) : Minecraft.getMinecraft().gameSettings.guiScale;
        if (newScale == 0) newScale = Minecraft.getMinecraft().gameSettings.guiScale;

        int oldScale = Minecraft.getMinecraft().gameSettings.guiScale;
        Minecraft.getMinecraft().gameSettings.guiScale = newScale;
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        Minecraft.getMinecraft().gameSettings.guiScale = oldScale;

        if (guiScales.size() > 0) {
            GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        } else {
            if (Loader.isModLoaded("labymod") && projectionMatrixOld.limit() > 0 && modelviewMatrixOld.limit() > 0) {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GL11.glLoadMatrix(projectionMatrixOld);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadMatrix(modelviewMatrixOld);
            } else {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            }
        }

        lastScale = scaledresolution;
        return scaledresolution;
    }

    public static void drawStringCentered(String str, FontRenderer fr, float x, float y, boolean shadow, int colour) {
        int strLen = fr.getStringWidth(str);

        float x2 = x - strLen / 2f;
        float y2 = y - fr.FONT_HEIGHT / 2f;

        GL11.glTranslatef(x2, y2, 0);
        fr.drawString(str, 0, 0, colour, shadow);
        GL11.glTranslatef(-x2, -y2, 0);
    }

    public static void renderWaypointText(String str, BlockPos loc, float partialTicks) {
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.pushMatrix();

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double x = loc.getX() - viewerX;
        double y = loc.getY() - viewerY - viewer.getEyeHeight();
        double z = loc.getZ() - viewerZ;

        double distSq = x * x + y * y + z * z;
        double dist = Math.sqrt(distSq);
        if (distSq > 144) {
            x *= 12 / dist;
            y *= 12 / dist;
            z *= 12 / dist;
        }
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0, viewer.getEyeHeight(), 0);

        drawNametag(str);

        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0, -0.25f, 0);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);

        drawNametag(EnumChatFormatting.YELLOW.toString() + Math.round(dist) + "m");

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
    }

    public static void drawNametag(String str) {
        FontRenderer fontrenderer = Minecraft.getMinecraft().fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-j - 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-j - 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
        GlStateManager.depthMask(true);

        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static String formattedNumber(int number, int numberToFormatAt) {
        DecimalFormat formatter = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.CANADA));
        formatter.setRoundingMode(RoundingMode.FLOOR);
        return number > numberToFormatAt - 1 ? formatter.format((double) number / 1000) + "k" : String.valueOf(number);
    }

    public static boolean equalsIgnoreCaseAnyOf(String string, String... strings) {
        for (String o : strings) if (string.equalsIgnoreCase(o)) return true;
        return false;
    }

    public static String getItemCustomId(ItemStack stack) {
        if (stack == null) return null;
        if (!stack.hasTagCompound()) return null;
        if (!stack.getTagCompound().hasKey("ExtraAttributes")) return null;
        if (!stack.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("id")) return null;
        return stack.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");
    }
}
