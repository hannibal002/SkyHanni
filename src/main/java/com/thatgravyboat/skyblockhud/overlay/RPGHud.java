package com.thatgravyboat.skyblockhud.overlay;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.thatgravyboat.skyblockhud.GuiTextures;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.core.config.Position;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
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
        mana =
            isIncrease
                ? Math.min(mana + (maxMana / 50), maxMana)
                : mana - decrease;
    }

    private static final DecimalFormat decimalFormat = new DecimalFormat(
        "#.##"
    );

    static {
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (
            Utils.overlayShouldRender(
                event.type,
                SkyblockHud.hasSkyblockScoreboard(),
                SkyblockHud.config.renderer.hideXpBar
            )
        ) MinecraftForge.EVENT_BUS.post(
            new RenderGameOverlayEvent.Post(
                new RenderGameOverlayEvent(
                    event.partialTicks,
                    event.resolution
                ),
                RenderGameOverlayEvent.ElementType.EXPERIENCE
            )
        );
        if (
            Utils.overlayShouldRender(
                event.type,
                SkyblockHud.hasSkyblockScoreboard(),
                SkyblockHud.config.rpg.showRpgHud
            )
        ) {
            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            FontRenderer font = mc.fontRendererObj;
            if (mc.thePlayer.getHealth() < mc.thePlayer.getMaxHealth()) {
                health =
                    Math.max(
                        (int) (
                            maxHealth *
                            (
                                mc.thePlayer.getHealth() /
                                mc.thePlayer.getMaxHealth()
                            )
                        ),
                        health
                    );
            }

            mc.renderEngine.bindTexture(GuiTextures.playerStat);
            Position position = SkyblockHud.config.rpg.rpgHudPosition;

            int x = position.getAbsX(event.resolution, 120);
            int y = position.getAbsY(event.resolution, 47);

            boolean rightAligned = position.rightAligned(event.resolution, 120);

            drawTexturedModalRect(x, y, rightAligned ? 131 : 5, 6, 120, 47);

            float manaWidth = Math.min(
                57 * ((float) mana / (float) maxMana),
                57
            );
            drawTexturedModalRect(
                rightAligned ? x + 16 : 47 + x,
                17 + y,
                rightAligned ? 199 : 0,
                64,
                (int) manaWidth,
                4
            );

            float healthWidth = Math.min(
                70 * ((float) health / (float) maxHealth),
                70
            );
            drawTexturedModalRect(
                rightAligned ? x + 3 : 47 + x,
                22 + y,
                rightAligned ? 186 : 0,
                68,
                (int) healthWidth,
                5
            );

            if (health > maxHealth) {
                float absorptionWidth = Math.min(
                    70 * ((float) (health - maxHealth) / (float) maxHealth),
                    70
                );
                drawTexturedModalRect(
                    rightAligned ? x + 3 : 47 + x,
                    22 + y,
                    rightAligned ? 186 : 0,
                    77,
                    (int) absorptionWidth,
                    5
                );
            }

            float xpWidth = 67 * mc.thePlayer.experience;
            drawTexturedModalRect(
                rightAligned ? x + 7 : 45 + x,
                28 + y,
                rightAligned ? 189 : 0,
                73,
                (int) xpWidth,
                4
            );
            //Air in water
            NumberFormat myFormat = NumberFormat.getInstance();
            myFormat.setGroupingUsed(true);
            if (mc.thePlayer.getAir() < 300) {
                float airWidth = 60 * ((float) mc.thePlayer.getAir() / 300);
                drawTexturedModalRect(
                    rightAligned ? x + 17 : 39 + x,
                    33 + y,
                    rightAligned ? 192 : 0,
                    82,
                    64,
                    6
                );
                drawTexturedModalRect(
                    rightAligned ? x + 19 : 41 + x,
                    33 + y,
                    rightAligned ? 196 : 0,
                    88,
                    (int) airWidth,
                    4
                );
            }
            GlStateManager.scale(0.75f, 0.75f, 1);
            drawCenteredString(
                mc.fontRendererObj,
                "" + mc.thePlayer.experienceLevel,
                (rightAligned ? 130 : 0) + (int) (15 + x / 0.75f),
                (int) (45 + y / 0.75f),
                8453920
            );
            GlStateManager.scale(1 / 0.75f, 1 / 0.75f, 1);
            GlStateManager.scale(0.75f, 0.75f, 1);
            font.drawString(
                ChatFormatting.RED + " \u2764 " + health + "/" + maxHealth,
                (rightAligned ? -40 : 0) + (int) (64 + x / 0.75f),
                (int) (8 + y / 0.75f),
                0xffffff,
                true
            );
            GlStateManager.scale(1 / 0.75f, 1 / 0.75f, 1);
            GlStateManager.color(255, 255, 255);
            GlStateManager.disableBlend();
        }
    }
}
