package at.lorenz.mod.config;

import at.lorenz.mod.LorenzMod;
import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.core.config.annotations.Category;
import com.thatgravyboat.skyblockhud.core.config.annotations.ConfigEditorBoolean;
import com.thatgravyboat.skyblockhud.core.config.annotations.ConfigOption;
import com.thatgravyboat.skyblockhud.core.config.gui.GuiPositionEditor;
import net.minecraft.client.Minecraft;

public class Features {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {}, () -> {}, () -> LorenzMod.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(LorenzMod.feature, activeConfig))));
    }

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
    @Category(name = "Item", desc = "Changing the behavior around items and the inventory.")
    public Inventory item = new Inventory();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public Bazaar bazaar = new Bazaar();

    public static class Chat {

        @Expose
        @ConfigOption(name = "Main Chat Filter", desc = "Hides all the annoying chat messages.")
        @ConfigEditorBoolean
        public boolean mainFilter = true;

        @Expose
        @ConfigOption(name = "Player Messages", desc = "Add a fancy new chat format for player messages.")
        @ConfigEditorBoolean
        public boolean playerMessages = false;
    }

    public static class Dungeon {

        @Expose
        @ConfigOption(name = "Hide Dungeon Messages", desc = "Hides annoyung dungeon messages.")
        @ConfigEditorBoolean
        public boolean hideAnnoyingMessages = false;

        @Expose
        @ConfigOption(name = "Highlight Clicked Blocks", desc = "Highlight blocks in dungeon when clicked: Lever, Chest, Wither Essence")
        @ConfigEditorBoolean
        public boolean highlightClickedBlocks = false;
    }

    public static class Inventory {

        @Expose
        @ConfigOption(name = "Not Clickable Items", desc = "Hide items that are not clickable in " + "the current inventory: ah, bz, accessory bag, etc")
        @ConfigEditorBoolean
        public boolean hideNotClickableItems = false;

        @Expose
        @ConfigOption(name = "Master Star Number", desc = "Shows the Tier of the Master Star.")
        @ConfigEditorBoolean
        public boolean displayMasterStarNumber = false;

        @Expose
        @ConfigOption(name = "Master Skull Number", desc = "Shows the tier of the Master Skull accessory.")
        @ConfigEditorBoolean
        public boolean displayMasterSkullNumber = false;

        @Expose
        @ConfigOption(name = "Dungeon Head Floor", desc = "Shows the correct floor for golden and diamond heads.")
        @ConfigEditorBoolean
        public boolean displayDungeonHeadFloor = false;

        @Expose
        @ConfigOption(name = "New Year Cake Number", desc = "Shows the Number of the Year of New Year Cakes.")
        @ConfigEditorBoolean
        public boolean displayNewYearCakeNumber = false;

        @Expose
        @ConfigOption(name = "Pet Level", desc = "Shows the level of the pet when not maxed.")
        @ConfigEditorBoolean
        public boolean displayPetLevel = false;

        @Expose
        @ConfigOption(name = "Sack Name", desc = "Shows an abbreviation of the Sack name.")
        @ConfigEditorBoolean
        public boolean displaySackName = false;

        @Expose
        @ConfigOption(name = "Minion Tier", desc = "Shows the Minion Tier over Items.")
        @ConfigEditorBoolean
        public boolean displayMinionTier = false;
    }

    public static class Bazaar {

        @Expose
        @ConfigOption(name = "Order Helper", desc = "Show visual hints when items are ready to pickup or outbid.")
        @ConfigEditorBoolean
        public boolean orderHelper = false;
    }
}
