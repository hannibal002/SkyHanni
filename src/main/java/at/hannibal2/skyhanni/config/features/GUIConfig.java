package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.data.GuiEditManager;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIConfig {

    @ConfigOption(name = "Edit GUI Locations", desc = "Change the position of SkyHanni's overlays.")
    @ConfigEditorButton(buttonText = "Edit")
    public Runnable positions = GuiEditManager::openGuiPositionEditor;

    @Expose
    @ConfigOption(name = "Open Hotkey", desc = "Press this key to open the GUI Editor.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindOpen = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Global GUI Scale", desc = "Globally scale all SkyHanni GUIs.")
    @ConfigEditorSlider(minValue = 0.1F, maxValue = 10, minStep = 0.05F)
    public float globalScale = 1F;


    @Expose
    @ConfigOption(name = "Modify Visual Words", desc = "")
    @Accordion
    public ModifyWords modifyWords = new ModifyWords();

    public static class ModifyWords {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enables replacing all instances of a word or phrase with another word or phrase.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Work Outside SkyBlock", desc = "Allows modifying visual words anywhere on Hypixel.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean workOutside = false;

        @ConfigOption(name = "Open Config", desc = "Opens the menu to setup the visual words.\n§eCommand: /shwords")
        @ConfigEditorButton(buttonText = "Open")
        public Runnable open = Commands::openVisualWords;

    }

    @Expose
    @ConfigOption(name = "Custom Text Box", desc = "")
    @Accordion
    public TextBoxConfig customTextBox = new TextBoxConfig();

    public static class TextBoxConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enables showing the textbox while in SkyBlock.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Text", desc = "Enter text you want to display here.\n" +
                "§eUse '&' as the colour code character.\n" +
                "§eUse '\\n' as the line break character.")
        @ConfigEditorText
        public Property<String> text = Property.of("&aYour Text Here\\n&bYour new line here");

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Custom Scoreboard", desc = "")
    @Accordion
    public GUIConfig.CustomScoreboard customScoreboard = new GUIConfig.CustomScoreboard();

    public static class CustomScoreboard {

        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Show a custom scoreboard instead of the default one." //TODO: MAKE COOLER
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(
            name = "Text Format",
            desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
            exampleText = {
                "§6§lSKYBLOCK",
                "§7♲ Blueberry",
                "Purse: §652,763,737",
                "Motes: §d64,647",
                "Bank: §6249M",
                "Bits: §b59,264",
                "Copper: §c23,495",
                "Gems: §a57,873",
                "",
                "§7⏣ §bVillage",
                "Late Summer 11th, Year 311",
                "§8m77CK",
                "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234",
                "",
                "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills",
                "§7Current Event",
                "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff",
                "",
                "Heat: §c♨ 0",
                "§9§lParty (4):\n §7- §fhannibal2\n §7- §f Moulberry\n §7- §f Vahvl\n §7- §f J10a1n15",
                "Power: Sighted",
                "§ewww.hypixel.net",
            }
        )
        public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20, 21));

        @Expose
        @ConfigOption(name = "Hide Vanilla Scoreboard", desc = "Hide the vanilla scoreboard.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideVanillaScoreboard = false;

        @Expose
        @ConfigOption(name = "Max Party List", desc = "Max number of party members to show in the party list. (You are not included)")
        @ConfigEditorSlider(
            minValue = 1,
            maxValue = 25, // why do I even set it so high
            minStep = 1
        )
        public Property<Integer> maxPartyList = Property.of(4);

        @Expose
        @ConfigOption(name = "Hide lines with no info", desc = "Hide lines that have no info to display, like hiding the party when not being in one.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideEmptyLines = true;

        @Expose
        @ConfigOption(name = "Hide Info not relevant to location", desc = "Hide lines that are not relevant to the current location, like hiding copper while not in garden")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideIrrelevantLines = true;

        @Expose
        @ConfigOption(name = "Display Numbers First", desc = "Determines whether the number or line name displays first. " +
            "§eNote: Will not update the preview above!")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean displayNumbersFirst = false;

        @Expose
        @ConfigOption(name = "Show Mayor Perks", desc = "Show the perks of the current mayor.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showMayorPerks = true;

        @Expose
        @ConfigOption(name = "Hide consecutive empty lines", desc = "Hide lines that are empty and have an empty line above them.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideConsecutiveEmptyLines = true;

        @Expose
        @ConfigOption(name = "Custom Title", desc = "What should be displayed as the title of the scoreboard.\nUse & for colors")
        @ConfigEditorText
        public Property<String> customTitle = Property.of("&6&lSKYBLOCK");

        @Expose
        @ConfigOption(name = "Custom Footer", desc = "What should be displayed as the footer of the scoreboard.\nUse & for colors")
        @ConfigEditorText
        public Property<String> customFooter = Property.of("&ewww.hypixel.net");

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Real Time", desc = "Display the current computer time, a handy feature when playing in full-screen mode.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean realTime = false;

    @Expose
    @ConfigOption(name = "Real Time 12h Format", desc = "Display the current computer time in 12hr Format rather than 24h Format.")
    @ConfigEditorBoolean
    public boolean realTimeFormatToggle = false;

    @Expose
    public Position realTimePosition = new Position(10, 10, false, true);


    @Expose
    @ConfigOption(name = "In-Game Date", desc = "")
    @Accordion
    public InGameDateConfig inGameDate = new InGameDateConfig();

    public static class InGameDateConfig {

        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Show the in-game date of SkyBlock (like in Apec, §ebut with mild delays§7).\n" +
                "(Though this one includes the SkyBlock year!)"
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        public Position position = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(
            name = "Use Scoreboard for Date",
            desc = "Uses the scoreboard instead to find the current month, date, and time. Greater \"accuracy\", depending on who's asking."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean useScoreboard = true;

        @Expose
        @ConfigOption(
            name = "Show Sun/Moon",
            desc = "Show the sun or moon symbol seen on the scoreboard."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean includeSunMoon = true;

        @Expose
        @ConfigOption(
            name = "Show Date Ordinal",
            desc = "Show the date's ordinal suffix. Ex: (1st <-> 1, 22nd <-> 22, 23rd <-> 3, 24th <-> 24, etc.)"
        )
        @ConfigEditorBoolean
        //@FeatureToggle
        public boolean includeOrdinal = false;

        @Expose
        @ConfigOption(
            name = "Refresh Rate",
            desc = "Change the time in seconds you would like to refresh the In-Game Date Display." +
                "\n§eNOTE: If \"Use Scoreboard for Date\" is enabled, this setting is ignored."
        )
        @ConfigEditorSlider(
            minValue = 1,
            maxValue = 60,
            minStep = 1
        )
        public int refreshSeconds = 30;
    }


    @Expose
    @ConfigOption(name = "TPS Display", desc = "Show the TPS of the current server, like in Soopy.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean tpsDisplay = false;

    @Expose
    public Position tpsDisplayPosition = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean configButtonOnPause = true;
}
