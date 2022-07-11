package at.lorenz.mod.config;

import at.lorenz.mod.LorenzMod;
import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud_2.config.SBHConfigEditor;
import com.thatgravyboat.skyblockhud_2.core.GuiElement;
import com.thatgravyboat.skyblockhud_2.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud_2.core.config.Position;
import com.thatgravyboat.skyblockhud_2.core.config.annotations.*;
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
            editOverlay(activeConfigCategory, 200, 16, debug.testPos);
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
    @Category(name = "Items", desc = "Changing the behavior around items and the inventory.")
    public Items items = new Items();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public Bazaar bazaar = new Bazaar();

    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Debug", desc = "Debug and test stuff.")
    public Debug debug = new Debug();

    @Expose
    @Category(name = "Abilities", desc = "Stuff about abilities.")
    public Abilities abilities = new Abilities();

    public static class Chat {

        @Expose
        @ConfigOption(name = "Chat Filter Types", desc = "")
        @ConfigEditorAccordion(id = 1)
        public boolean filterTypes = false;

        @Expose
        @ConfigOption(name = "HyPixel Hub", desc = "Block messages outside SkyBlock in the HyPixel lobby: player joins, loot boxes, prototype lobby messages, radiating generosity and HyPixel tournaments.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean hypixelHub = false;

        @Expose
        @ConfigOption(name = "Empty", desc = "Hide all the empty messages from the chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean empty = false;

        @Expose
        @ConfigOption(name = "Warping", desc = "Block 'sending request to join ..' and 'warping ..' messages.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean warping = false;

        @Expose
        @ConfigOption(name = "Welcome", desc = "Hide the 'welcome to skyblock' message.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean welcome = false;

        //TODO remove
        @Expose
        @ConfigOption(name = "Others", desc = "Hide other annoying messages.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean others = false;

        @Expose
        @ConfigOption(name = "Player Messages", desc = "Add a fancy new chat format for player messages.")
        @ConfigEditorBoolean
        public boolean playerMessages = false;

        @Expose
        @ConfigOption(name = "Dungeon Filter", desc = "Hide annoying messages inside dungeon.")
        @ConfigEditorBoolean
        public boolean dungeonMessages = false;
    }

    public static class Dungeon {

        @Expose
        @ConfigOption(name = "Clicked Blocks", desc = "Highlight the following blocks when clicked in dungeon: Lever, Chest, Wither Essence")
        @ConfigEditorBoolean
        public boolean highlightClickedBlocks = false;

        @Expose
        @ConfigOption(name = "Boss Damage Indicator", desc = "Shows the missing health of a boss in the dungeon and the cooldown time until the boss becomes attackable.")
        @ConfigEditorBoolean
        public boolean bossDamageIndicator = false;
    }

    public static class Items {

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
        @ConfigOption(name = "New Year Cake", desc = "Shows the Number of the Year of New Year Cakes.")
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
        @ConfigOption(name = "Order Helper", desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid.")
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

        @Expose
        @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience bottles lying on the ground.")
        @ConfigEditorBoolean
        public boolean hideExpBottles = false;

        @Expose
        @ConfigOption(name = "Config Button", desc = "Adds a button to the pause menu to configure the Lorenz mod.")
        @ConfigEditorBoolean
        public boolean configButtonOnPause = true;
    }

    public static class Debug {

        @Expose
        @ConfigOption(name = "Enable Test", desc = "Enable Test logic")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Test Location", desc = "")
        @ConfigEditorButton(runnableId = "testPos", buttonText = "Edit")
        public Position testPos = new Position(10, 10, false, true);
    }

    public static class Abilities {

        @Expose
        @ConfigOption(name = "Item Cooldown", desc = "Shows the cooldown of item abilities.")
        @ConfigEditorBoolean
        public boolean itemAbilityCooldown = false;
    }
}
