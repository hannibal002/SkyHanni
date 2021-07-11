package com.thatgravyboat.skyblockhud.config;

import com.google.gson.annotations.Expose;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.core.GuiScreenElementWrapper;
import com.thatgravyboat.skyblockhud.core.config.Config;
import com.thatgravyboat.skyblockhud.core.config.Position;
import com.thatgravyboat.skyblockhud.core.config.annotations.*;
import com.thatgravyboat.skyblockhud.core.config.gui.GuiPositionEditor;
import net.minecraft.client.Minecraft;

public class SBHConfig extends Config {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {}, () -> {}, () -> SkyblockHud.screenToOpen = new GuiScreenElementWrapper(new SBHConfigEditor(SkyblockHud.config, activeConfig))));
    }

    @Override
    public void executeRunnable(String runnableId) {
        String activeConfigCategory = null;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
            if (wrapper.element instanceof SBHConfigEditor) {
                activeConfigCategory = ((SBHConfigEditor) wrapper.element).getSelectedCategoryName();
            }
        }

        switch (runnableId) {
            case "rpg":
                editOverlay(activeConfigCategory, 120, 47, rpg.rpgHudPosition);
                return;
            case "d1":
                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer1);
                return;
            case "d2":
                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer2);
                return;
            case "d3":
                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer3);
                return;
            case "d4":
                editOverlay(activeConfigCategory, 120, 32, dungeon.dungeonPlayer4);
                return;
            case "main":
                editOverlay(activeConfigCategory, 1000, 34, main.mainHudPos);
                return;
            case "ultimate":
                editOverlay(activeConfigCategory, 182, 5, dungeon.barPosition);
                return;
            case "map":
                editOverlay(activeConfigCategory, 72, 72, map.miniMapPosition);
                return;
            //            case "tracker":
            //                editOverlay(activeConfigCategory, 120, 70, trackers.trackerPosition);
            //                return;
        }
    }

    @Expose
    @Category(name = "Misc Options", desc = "Just a bunch of random options.")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Main Hud", desc = "All Options for the main hud.")
    public MainHud main = new MainHud();

    @Expose
    @Category(name = "RPG Hud", desc = "All Options for the RPG hud.")
    public RPGHud rpg = new RPGHud();

    @Expose
    @Category(name = "Dungeon Hud", desc = "All Options for the Dungeon hud.")
    public DungeonHud dungeon = new DungeonHud();

    @Expose
    @Category(name = "Renderer", desc = "All Options for rendering.")
    public Renderer renderer = new Renderer();

    @Expose
    @Category(name = "Map", desc = "All Options for the Map.")
    public Map map = new Map();

    @Expose
    @Category(name = "Mining Hud", desc = "All Options for the Mining Hud.")
    public MiningHud miningHud = new MiningHud();

    //TODO ENABLED WHEN TRACKER GETS ADDED BACK
    //    @Expose
    //    @Category(name = "Tracker", desc = "All Options for the Trackers.")
    //    public Trackers trackers = new Trackers();

    public static class Misc {

        @Expose
        @ConfigOption(name = "Hide Scoreboard", desc = "Hides the scoreboard when in skyblock.")
        @ConfigEditorBoolean
        public boolean hideScoreboard = false;
    }

    public static class MainHud {

        @Expose
        @ConfigOption(name = "Main Hud Position", desc = "")
        @ConfigEditorButton(runnableId = "main", buttonText = "Edit")
        public Position mainHudPos = new Position(0, 1, true, false);

        @Expose
        @ConfigOption(name = "Twelve Hour Clock", desc = "Allows you to change the clock to be 12 hour instead of 24 hour.")
        @ConfigEditorBoolean
        public boolean twelveHourClock = false;

        @Expose
        @ConfigOption(name = "Shift hud with boss", desc = "Shifts the hud when bossbar is visible.")
        @ConfigEditorBoolean
        public boolean bossShiftHud = true;

        @Expose
        @ConfigOption(name = "Require Redstone", desc = "Allows to make it so that the redstone percentage requires you to hold a redstone item to show.")
        @ConfigEditorBoolean
        public boolean requireRedstone = true;
    }

    public static class RPGHud {

        @Expose
        @ConfigOption(name = "Show RPG Hud", desc = "Allows you to show or hide the RPG Hud.")
        @ConfigEditorBoolean
        public boolean showRpgHud = true;

        @Expose
        @ConfigOption(name = "RPG Hud Position", desc = "Allows you to change the position of the RPG Hud.")
        @ConfigEditorButton(runnableId = "rpg", buttonText = "Edit")
        public Position rpgHudPosition = new Position(1, 1);
    }

    public static class DungeonHud {

        @Expose
        @ConfigOption(name = "Dungeon Ultimate Bar", desc = "")
        @ConfigEditorAccordion(id = 2)
        public boolean ultimateBar = false;

        @Expose
        @ConfigOption(name = "Hide Ultimate Bar", desc = "Hides the custom ultimate bar.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean hideUltimateBar = false;

        @Expose
        @ConfigOption(name = "Bar Position", desc = "Change the position of the bar.")
        @ConfigEditorButton(runnableId = "ultimate", buttonText = "Edit")
        @ConfigAccordionId(id = 2)
        public Position barPosition = new Position(0, 50, true, false);

        @Expose
        @ConfigOption(name = "Bar Loading Color", desc = "The color of the bar when its loading.")
        @ConfigEditorColour
        @ConfigAccordionId(id = 2)
        public String barLoadColor = "159:0:0:0:255";

        @Expose
        @ConfigOption(name = "Bar Full Color", desc = "The color of the bar when its full.")
        @ConfigEditorColour
        @ConfigAccordionId(id = 2)
        public String barFullColor = "255:0:0:0:255";

        @Expose
        @ConfigOption(name = "Bar Style", desc = "Change the style of the bar")
        @ConfigEditorDropdown(values = { "No Notch", "6 Notch", "10 Notch", "12 Notch", "20 Notch" })
        @ConfigAccordionId(id = 2)
        public int barStyle = 2;

        @Expose
        @ConfigOption(name = "Dungeon Players", desc = "")
        @ConfigEditorAccordion(id = 1)
        public boolean dungeonPlayerAccordion = false;

        @Expose
        @ConfigOption(name = "Hide Dungeon Players", desc = "Allows you to hide the dungeon player hud")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean hideDungeonPlayers = false;

        @Expose
        @ConfigOption(name = "Dungeon Player Opacity", desc = "Allows you to change the opacity of the dungeon players.")
        @ConfigEditorSlider(minValue = 0, maxValue = 100, minStep = 1)
        @ConfigAccordionId(id = 1)
        public int dungeonPlayerOpacity = 0;

        @Expose
        @ConfigOption(name = "Hide Dead Players", desc = "Allows you to hide players that are dead or have left.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean hideDeadDungeonPlayers = false;

        @Expose
        @ConfigOption(name = "Player Position 1", desc = "Change the position of this dungeon player.")
        @ConfigEditorButton(runnableId = "d1", buttonText = "Edit")
        @ConfigAccordionId(id = 1)
        public Position dungeonPlayer1 = new Position(5, 5);

        @Expose
        @ConfigOption(name = "Player Position 2", desc = "Change the position of this dungeon player.")
        @ConfigEditorButton(runnableId = "d2", buttonText = "Edit")
        @ConfigAccordionId(id = 1)
        public Position dungeonPlayer2 = new Position(5, 42);

        @Expose
        @ConfigOption(name = "Player Position 3", desc = "Change the position of this dungeon player.")
        @ConfigEditorButton(runnableId = "d3", buttonText = "Edit")
        @ConfigAccordionId(id = 1)
        public Position dungeonPlayer3 = new Position(5, 79);

        @Expose
        @ConfigOption(name = "Player Position 4", desc = "Change the position of this dungeon player.")
        @ConfigEditorButton(runnableId = "d4", buttonText = "Edit")
        @ConfigAccordionId(id = 1)
        public Position dungeonPlayer4 = new Position(5, 116);
    }

    public static class Renderer {

        @Expose
        @ConfigOption(name = "Hide Boss Bar", desc = "Hides Boss Bar when certain conditions are met such as the name is just wither or it starts with objective:")
        @ConfigEditorBoolean
        public boolean hideBossBar = true;

        @Expose
        @ConfigOption(name = "Hide XP Bar", desc = "Hides xp bar.")
        @ConfigEditorBoolean
        public boolean hideXpBar = true;

        @Expose
        @ConfigOption(name = "Hide Food", desc = "Hides food.")
        @ConfigEditorBoolean
        public boolean hideFood = true;

        @Expose
        @ConfigOption(name = "Hide air", desc = "Hides air.")
        @ConfigEditorBoolean
        public boolean hideAir = true;

        @Expose
        @ConfigOption(name = "Hide hearts", desc = "Hides hearts.")
        @ConfigEditorBoolean
        public boolean hideHearts = true;

        @Expose
        @ConfigOption(name = "Hide armor", desc = "Hides armor.")
        @ConfigEditorBoolean
        public boolean hideArmor = true;

        @Expose
        @ConfigOption(name = "Hide Animal Hearts", desc = "Hides Animal Hearts.")
        @ConfigEditorBoolean
        public boolean hideAnimalHearts = true;
    }

    public static class Map {

        @Expose
        @ConfigOption(name = "Show Player Location", desc = "This feature is off by default as Hypixel's rules are so vague that this would fall under their disallowed modifications.")
        @ConfigEditorBoolean
        public boolean showPlayerLocation = false;

        @Expose
        @ConfigOption(name = "Show Mini-Map", desc = "Shows the Mini-Map on your overlay if turned off you can still use /sbhmap to see the map in fullscreen.")
        @ConfigEditorBoolean
        public boolean showMiniMap = false;

        @Expose
        @ConfigOption(name = "Mini-Map Position", desc = "Allows you to change the position of the Mini-Map.")
        @ConfigEditorButton(runnableId = "map", buttonText = "Edit")
        public Position miniMapPosition = new Position(0, 100, false, false);

        @Expose
        @ConfigOption(name = "Icons", desc = "")
        @ConfigEditorAccordion(id = 3)
        public boolean icons = false;

        @Expose
        @ConfigOption(name = "NPC", desc = "Show NPC Icons")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean showNpcIcons = true;

        @Expose
        @ConfigOption(name = "Info", desc = "Show Info Icons")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean showInfoIcons = true;

        @Expose
        @ConfigOption(name = "Misc", desc = "Show Misc Icons")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean showMiscIcons = true;

        @Expose
        @ConfigOption(name = "Shops", desc = "Show Shop Icons")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean showShopIcons = true;

        @Expose
        @ConfigOption(name = "Quests", desc = "Show Quest Icons")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean showQuestIcons = false;
    }

    public static class MiningHud {

        @Expose
        @ConfigOption(name = "Show Drill Bar", desc = "Allows you to show or hide the Drill Bar.")
        @ConfigEditorBoolean
        public boolean showDrillBar = true;

        @Expose
        @ConfigOption(name = "Show Heat Bar", desc = "Allows you to show or hide the Heat Bar.")
        @ConfigEditorBoolean
        public boolean showHeatBar = true;
    }

    public static class Trackers {

        @Expose
        @ConfigOption(name = "Tracker Position", desc = "Allows you to change the position of the Trackers.")
        @ConfigEditorButton(runnableId = "tracker", buttonText = "Edit")
        public Position trackerPosition = new Position(-1, 200);

        @Expose
        @ConfigOption(name = "Hide Tracker", desc = "It will still track the data just in case.")
        @ConfigEditorBoolean
        public boolean hideTracker = false;
    }
}
