package at.lorenz.mod.config;

import at.lorenz.mod.LorenzMod;
import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud_2.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud_2.core.GuiElement;
import com.thatgravyboat.skyblockhud_2.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud_2.core.config.Position;
import com.thatgravyboat.skyblockhud_2.core.config.annotations.Category;
import com.thatgravyboat.skyblockhud_2.core.config.annotations.ConfigEditorBoolean;
import com.thatgravyboat.skyblockhud_2.core.config.annotations.ConfigEditorButton;
import com.thatgravyboat.skyblockhud_2.core.config.annotations.ConfigOption;
import com.thatgravyboat.skyblockhud_2.core.config.gui.GuiPositionEditor;
import net.minecraft.client.Minecraft;

public class Features {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {
        }, () -> {
        }, () -> LorenzMod.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(LorenzMod.feature, activeConfig))));
    }

    public void executeRunnable(String runnableId) {
        String activeConfigCategory = null;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
            GuiElement element = wrapper.element;
            if (element instanceof SBHConfigEditor) {
                activeConfigCategory = ((SBHConfigEditor) element).getSelectedCategoryName();
            }
        }

        if (runnableId.equals("petDisplay")) {
            editOverlay(activeConfigCategory, 200, 16, misc.petDisplayPos);
            return;
        }

        if (runnableId.equals("testPos")) {
            editOverlay(activeConfigCategory, 200, 16, test.testPos);
            return;
        }
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

    @Expose
    @Category(name = "Misc", desc = "Settings without a big category")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Test", desc = "Test stuff")
    public Test test = new Test();

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

    public static class Misc {

        @Expose
        @ConfigOption(name = "Pet Display", desc = "Shows the current active pet.")
        @ConfigEditorBoolean
        public boolean petDisplay = false;

        @Expose
        @ConfigOption(name = "Pet Display Position", desc = "")
        @ConfigEditorButton(runnableId = "petDisplay", buttonText = "Edit")
        public Position petDisplayPos = new Position(10, 10, false, true);
    }

    public static class Test {

        @Expose
        @ConfigOption(name = "Enable Test", desc = "Enable Test logic")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Test Location", desc = "testPos")
        @ConfigEditorButton(runnableId = "testPos", buttonText = "Edit")
        public Position testPos = new Position(10, 10, false, true);
    }
}
