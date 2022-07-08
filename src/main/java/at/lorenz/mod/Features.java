package at.lorenz.mod;

import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud.LorenzMod;
import com.thatgravyboat.skyblockhud.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.core.config.Config;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.core.config.annotations.Category;
import com.thatgravyboat.skyblockhud.core.config.annotations.ConfigEditorBoolean;
import com.thatgravyboat.skyblockhud.core.config.annotations.ConfigOption;
import com.thatgravyboat.skyblockhud.core.config.gui.GuiPositionEditor;
import net.minecraft.client.Minecraft;

public class Features extends Config {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {
        }, () -> {
        }, () -> LorenzMod.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(LorenzMod.config, activeConfig))));
    }

    @Override
    public void executeRunnable(String runnableId) {
//        String activeConfigCategory = null;
//        if (Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
//            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
//            if (wrapper.element instanceof SBHConfigEditor) {
//                activeConfigCategory = ((SBHConfigEditor) wrapper.element).getSelectedCategoryName();
//            }
//        }
//
//        switch (runnableId) {
//            case "rpg":
//                editOverlay(activeConfigCategory, 120, 47, rpg.rpgHudPosition);
//                return;
//            case "d1":
//                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer1);
//                return;
//            case "d2":
//                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer2);
//                return;
//            case "d3":
//                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer3);
//                return;
//            case "d4":
//                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer4);
//                return;
//            case "main":
//                editOverlay(activeConfigCategory, 1000, 34, main.mainHudPos);
//                return;
//            case "ultimate":
//                editOverlay(activeConfigCategory, 182, 5, dungeon.barPosition);
//                return;
//            case "map":
//                editOverlay(activeConfigCategory, 72, 72, map.miniMapPosition);
//                return;
//            case "tracker":
//                editOverlay(activeConfigCategory, 130, 70, trackers.trackerPosition);
//                return;
//            case "drill":
//                editOverlay(activeConfigCategory, 136, 7, mining.drillBar);
//                return;
//            case "heat":
//                editOverlay(activeConfigCategory, 45, 7, mining.heatBar);
//                return;
//            case "dialogue":
//                editOverlay(activeConfigCategory, 182, 68, misc.dialoguePos);
//                return;
//        }
    }


    @Expose
    @Category(name = "Chat", desc = "Chat related features.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the catacombs dungeon experience.")
    public Dungeon dungeon = new Dungeon();

    @Expose
    @Category(name = "Inventory", desc = "Changing the behavior around the inventory.")
    public Inventory inventory = new Inventory();

    public static class Chat {

        @Expose
        @ConfigOption(name = "Main Chat Filter", desc = "Hides all the annoying chat messages.")
        @ConfigEditorBoolean
        public boolean filter = false;
    }

    public static class Dungeon {

        @Expose
        @ConfigOption(name = "Hide Dungeon Messages", desc = "Hides annoyung dungeon messages.")
        @ConfigEditorBoolean
        public boolean hideAnnoyingMessages = false;
    }

    public static class Inventory {

        @Expose
        @ConfigOption(name = "Hide Not Clickable Items", desc = "Hide items that are not clickable in " +
                "the current inventory: ah, bz, accessory bag, etc")
        @ConfigEditorBoolean
        public boolean hideNotClickableItems = false;
    }
}
