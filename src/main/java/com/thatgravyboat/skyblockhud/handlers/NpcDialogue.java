package com.thatgravyboat.skyblockhud.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import at.lorenz.mod.LorenzMod;
import com.thatgravyboat.skyblockhud.textures.Textures;
import com.thatgravyboat.skyblockhud.utils.Utils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class NpcDialogue implements IResourceManagerReloadListener {

    public static final Pattern NPC_DIALOGUE_REGEX = Pattern.compile("\\[NPC] (.*): (.*)");

    private static final Gson gson = new GsonBuilder().create();
    private static final Map<String, ResourceLocation> NPCS = new HashMap<>();

    private static boolean showDialogue = false;
    private static int ticks = 0;

    private static final Queue<Dialogue> DIALOGUE = new ArrayDeque<>();
    private static Dialogue currentDialogue = null;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START) || LorenzMod.config.misc.hideDialogueBox) return;
        if (showDialogue) ticks++; else ticks = 0;

        if (showDialogue && ticks % 60 == 0) {
            currentDialogue = DIALOGUE.poll();

            if (currentDialogue == null) {
                showDialogue = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 2 && !LorenzMod.config.misc.hideDialogueBox) {
            String message = Utils.removeColor(event.message.getUnformattedText());
            if (message.toLowerCase(Locale.ENGLISH).startsWith("[npc]")) {
                Matcher matcher = NPC_DIALOGUE_REGEX.matcher(message);
                if (matcher.find()) {
                    showDialogue = true;
                    event.setCanceled(true);

                    Dialogue dialogue = new Dialogue(matcher.group(1), matcher.group(2));
                    if (currentDialogue == null) currentDialogue = dialogue; else DIALOGUE.add(dialogue);
                }
            }
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (Utils.overlayShouldRender(event.type, LorenzMod.hasSkyblockScoreboard(), showDialogue, !LorenzMod.config.misc.hideDialogueBox)) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.renderEngine.bindTexture(Textures.texture.dialogue);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

            int x = LorenzMod.config.misc.dialoguePos.getAbsX(event.resolution, 182) - 91;
            int y = LorenzMod.config.misc.dialoguePos.getAbsY(event.resolution, 68);

            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 182, 68, 256, 256);

            String npcID = currentDialogue.name.toLowerCase(Locale.ENGLISH).replace(" ", "_");

            if (NPCS.containsKey(npcID)) {
                mc.renderEngine.bindTexture(NPCS.get(npcID));
                Gui.drawModalRectWithCustomSizedTexture(x + 4, y + 4, 0, 0, 32, 60, 128, 128);
            }

            FontRenderer font = mc.fontRendererObj;

            font.drawString(currentDialogue.name, x + 40, y + 10, 0xffffff);

            for (int i = 0; i < currentDialogue.dialogue.size(); i++) {
                Utils.drawStringScaled(currentDialogue.dialogue.get(i), font, x + 40, y + 10 + font.FONT_HEIGHT + 6 + (i * font.FONT_HEIGHT + 3), false, 0xffffff, 0.75f);
            }
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        NPCS.clear();
        try {
            ResourceLocation npcs = new ResourceLocation("skyblockhud:data/npc_textures.json");
            InputStream is = resourceManager.getResource(npcs).getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                for (JsonElement npc : gson.fromJson(reader, JsonObject.class).getAsJsonArray("npcs")) {
                    JsonObject npcObject = npc.getAsJsonObject();
                    String npcName = npcObject.get("name").getAsString();
                    ResourceLocation rl = new ResourceLocation(npcObject.get("texture").getAsString());
                    NPCS.put(npcName.toLowerCase(Locale.ENGLISH).replace(" ", "_"), rl);
                }
            }
        } catch (Exception ignored) {}
    }

    static class Dialogue {

        public List<String> dialogue;
        public String name;

        public Dialogue(String name, String dialogue) {
            this.dialogue = Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(dialogue, 160);
            this.name = name;
        }
    }
}
