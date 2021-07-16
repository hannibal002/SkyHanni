package com.thatgravyboat.skyblockhud.handlers;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static com.thatgravyboat.skyblockhud.GuiTextures.dialogue;

public class NpcDialogue {

    public static final Pattern NPC_DIALOGUE_REGEX = Pattern.compile("\\[NPC] (.*): (.*)");

    private static boolean showDialogue = false;
    private static int ticks = 0;

    private static final Queue<String> DIALOGUE = new ArrayDeque<>();
    private static String currentNpc = "Unknown";
    private static String currentDialogue = null;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        if (event.phase.equals(TickEvent.Phase.START) || SkyblockHud.config.misc.hideDialogueBox) return;
        if (showDialogue) ticks++;
        else ticks = 0;

        if (showDialogue && ticks % 60 == 0){
            currentDialogue = DIALOGUE.poll();

            if (currentDialogue == null) {
                showDialogue = false;
                currentNpc = "Unknown";
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event){
        if (event.type != 2 && !SkyblockHud.config.misc.hideDialogueBox){
            String message = Utils.removeColor(event.message.getUnformattedText());
            if (message.toLowerCase(Locale.ENGLISH).startsWith("[npc]")){
                Matcher matcher = NPC_DIALOGUE_REGEX.matcher(message);
                if (matcher.find()) {
                    showDialogue = true;
                    event.setCanceled(true);
                    currentNpc = matcher.group(1);
                    if (currentDialogue != null) {
                        DIALOGUE.add(matcher.group(2));
                    } else {
                        currentDialogue = matcher.group(2);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, SkyblockHud.hasSkyblockScoreboard(), showDialogue, !SkyblockHud.config.misc.hideDialogueBox)) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.renderEngine.bindTexture(dialogue);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

            int x = SkyblockHud.config.misc.dialoguePos.getAbsX(event.resolution,182) - 91;
            int y = SkyblockHud.config.misc.dialoguePos.getAbsY(event.resolution,68);

            Gui.drawModalRectWithCustomSizedTexture(x,y, 0, 0, 182, 68, 256, 256);

            FontRenderer font = mc.fontRendererObj;

            font.drawString(currentNpc, x + 40, y + 10, 0xffffff);

            List<String> text = font.listFormattedStringToWidth(currentDialogue, 160);

            for (int i = 0; i < text.size(); i++) {
                Utils.drawStringScaled(text.get(i), font, x + 40,  y + 10 + font.FONT_HEIGHT + 6 + (i * font.FONT_HEIGHT + 3), false, 0xffffff, 0.75f);
            }
        }
    }








}
