package at.lorenz.mod.config;

import at.lorenz.mod.LorenzMod;
import at.lorenz.mod.config.config.ConfigEditor;
import at.lorenz.mod.config.core.GuiElement;
import at.lorenz.mod.config.core.GuiScreenElementWrapper;
import at.lorenz.mod.config.core.config.Position;
import at.lorenz.mod.config.core.config.annotations.*;
import at.lorenz.mod.config.core.config.gui.GuiPositionEditor;
import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;

public class Features {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {}, () -> {}, () -> LorenzMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(LorenzMod.feature, activeConfig))));
    }

    public void executeRunnable(String runnableId) {
        String activeConfigCategory = null;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
            GuiElement element = wrapper.element;
            if (element instanceof ConfigEditor) {
                activeConfigCategory = ((ConfigEditor) element).getSelectedCategoryName();
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

        if (runnableId.equals("dungeonMilestoneDisplay")) {
            editOverlay(activeConfigCategory, 200, 16, dungeon.milestoneDisplayPos);
            return;
        }

        if (runnableId.equals("dungeonDeathCounter")) {
            editOverlay(activeConfigCategory, 200, 16, dungeon.deathCounterDisplay);
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

        @Expose
        @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from watcher and bosses inside dungeon.")
        @ConfigEditorBoolean
        public boolean dungeonBossMessages = false;
    }

    public static class Dungeon {

        @Expose
        @ConfigOption(name = "Clicked Blocks", desc = "Highlight the following blocks when clicked in dungeon: Lever, Chest, Wither Essence")
        @ConfigEditorBoolean
        public boolean highlightClickedBlocks = false;

        @Expose
        @ConfigOption(name = "Boss Damage Indicator", desc = "Show the missing health of a boss in the dungeon and the cooldown time until the boss becomes attackable.")
        @ConfigEditorBoolean
        public boolean bossDamageIndicator = false;

        @Expose
        @ConfigOption(name = "Milestone Display", desc = "Show the current milestone inside Dungeons.")
        @ConfigEditorBoolean
        public boolean showMilestoneDisplay = false;

        @Expose
        @ConfigOption(name = "Milestone Display Position", desc = "")
        @ConfigEditorButton(runnableId = "dungeonMilestoneDisplay", buttonText = "Edit")
        public Position milestoneDisplayPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Death Counter", desc = "Display the total amount of deaths in the current dungeon.")
        @ConfigEditorBoolean
        public boolean deathCounter = false;

        @Expose
        @ConfigOption(name = "Death Counter Position", desc = "")
        @ConfigEditorButton(runnableId = "dungeonDeathCounter", buttonText = "Edit")
        public Position deathCounterDisplay = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Clean End", desc = "Hide entities and particles after the boss in Floor 1 - 6 has died.")
        @ConfigEditorBoolean
        public boolean cleanEnd = false;

        @Expose
        @ConfigOption(name = "Ignore Guardians", desc = "Ignore F3 and M3 guardians from the clean end feature when sneaking. Makes it easier to kill them after the boss died already. Thanks hypixel.")
        @ConfigEditorBoolean
        public boolean cleanEndF3IgnoreGuardians = false;
    }

    public static class Items {

        @Expose
        @ConfigOption(name = "Not Clickable Items", desc = "Hide items that are not clickable in " + "the current inventory: ah, bz, accessory bag, etc")
        @ConfigEditorBoolean
        public boolean hideNotClickableItems = false;

        @Expose
        @ConfigOption(name = "Item number as stack size", desc = "")
        @ConfigEditorAccordion(id = 2)
        public boolean filterTypes = false;

        @Expose
        @ConfigOption(name = "Master Star Number", desc = "Show the Tier of the Master Star.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayMasterStarNumber = false;

        @Expose
        @ConfigOption(name = "Master Skull Number", desc = "Show the tier of the Master Skull accessory.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayMasterSkullNumber = false;

        @Expose
        @ConfigOption(name = "Dungeon Head Floor", desc = "Show the correct floor for golden and diamond heads.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayDungeonHeadFloor = false;

        @Expose
        @ConfigOption(name = "New Year Cake", desc = "Show the Number of the Year of New Year Cakes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayNewYearCakeNumber = false;

        @Expose
        @ConfigOption(name = "Pet Level", desc = "Show the level of the pet when not maxed.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayPetLevel = false;

        @Expose
        @ConfigOption(name = "Sack Name", desc = "Show an abbreviation of the Sack name.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displaySackName = false;

        @Expose
        @ConfigOption(name = "Minion Tier", desc = "Show the Minion Tier over Items.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean displayMinionTier = false;

        @Expose
        @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
        @ConfigEditorBoolean
        public boolean itemAbilityCooldown = false;
    }

    public static class Bazaar {

        @Expose
        @ConfigOption(name = "Order Helper", desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid.")
        @ConfigEditorBoolean
        public boolean orderHelper = false;
    }

    public static class Misc {

        @Expose
        @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
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
        @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure the Lorenz mod.")
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
}
