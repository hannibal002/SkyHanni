package com.thatgravyboat.skyblockhud.overlay;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.handlers.HeldItemHandler;
import com.thatgravyboat.skyblockhud.textures.Textures;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RPGHud extends Gui {

    private static int mana, maxMana, overflow = 0;
    private static int health, maxHealth = 0;
    private static int defense = 0;

    public static void updateMana(int current, int max) {
        mana = current;
        maxMana = max;
    }

    public static void updateOverflow(int current) {
        overflow = current;
    }

    public static void updateHealth(int current, int max) {
        health = current;
        maxHealth = max;
    }

    public static void updateDefense(int input) {
        defense = input;
    }

    public static void manaPredictionUpdate(boolean isIncrease, int decrease) {
        mana = isIncrease ? Math.min(mana + (maxMana / 50), maxMana) : mana - decrease;
    }

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    static {
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), SkyblockHud.config.rpg.showRpgHud)) {
            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            FontRenderer font = mc.fontRendererObj;
            if (mc.thePlayer.getHealth() < mc.thePlayer.getMaxHealth()) {
                health = Math.max((int) (maxHealth * (mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth())), health);
            }

            mc.renderEngine.bindTexture(Textures.texture.playerStats);
            Position position = SkyblockHud.config.rpg.rpgHudPosition;

            int x = position.getAbsX(event.resolution, 120);
            int y = position.getAbsY(event.resolution, 47);

            boolean rightAligned = position.rightAligned(event.resolution, 120) && SkyblockHud.config.rpg.flipHud;

            drawTexturedModalRect(x, y, rightAligned ? 131 : 5, 6, 120, 47);

            float manaWidth = Math.min(57 * ((float) mana / (float) maxMana), 57);
            int manaX = rightAligned ? x + 16 : 47 + x;
            if (HeldItemHandler.hasManaCost(mc.thePlayer.getHeldItem())) {
                int manaCost = HeldItemHandler.getManaCost(mc.thePlayer.getHeldItem());
                drawTexturedModalRect(manaX, 17 + y, rightAligned ? 199 : 0, manaCost > mana ? 96 : 64, (int) manaWidth, 4);
                if (manaCost <= mana) {
                    drawTexturedModalRect(manaX, 17 + y, rightAligned ? 199 : 0, 92, Utils.lerp((float) manaCost / (float) maxMana, 0, 57), 4);
                }
            } else {
                drawTexturedModalRect(manaX, 17 + y, rightAligned ? 199 : 0, 64, (int) manaWidth, 4);
            }

            float healthWidth = Math.min(70 * ((float) health / (float) maxHealth), 70);
            int healthX = rightAligned ? x + 3 : 47 + x;
            drawTexturedModalRect(healthX, 22 + y, rightAligned ? 186 : 0, 68, (int) healthWidth, 5);

            if (health > maxHealth) {
                float absorptionWidth = Math.min(70 * ((float) (health - maxHealth) / (float) maxHealth), 70);
                drawTexturedModalRect(healthX, 22 + y, rightAligned ? 186 : 0, 77, (int) absorptionWidth, 5);
            }

            drawTexturedModalRect(rightAligned ? x + 7 : 45 + x, 28 + y, rightAligned ? 189 : 0, 73, Utils.lerp(mc.thePlayer.experience, 0, 67), 4);
            //Air in water
            NumberFormat myFormat = NumberFormat.getInstance();
            myFormat.setGroupingUsed(true);
            if (mc.thePlayer.getAir() < 300) {
                drawTexturedModalRect(rightAligned ? x + 17 : 39 + x, 33 + y, rightAligned ? 192 : 0, 82, 64, 6);
                drawTexturedModalRect(rightAligned ? x + 19 : 41 + x, 33 + y, rightAligned ? 196 : 0, 88, Utils.lerp(mc.thePlayer.getAir() / 300f, 0, 60), 4);
            }

            Utils.drawStringScaled("" + mc.thePlayer.experienceLevel, font, (rightAligned ? 112 : 14) + x - (font.getStringWidth("" + mc.thePlayer.experienceLevel) / 2f), 34 + y, false, 8453920, 0.75f);

            Utils.drawStringScaled(ChatFormatting.RED + " \u2764 " + health + "/" + maxHealth, font, (rightAligned ? 10 : 42) + x, 8 + y, true, 0xffffff, 0.75f);

            GlStateManager.color(255, 255, 255);
            GlStateManager.disableBlend();
        }
    }
}
