package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomScoreboardConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a custom scoreboard instead of the default one."
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
            "Heat: §c♨ 0",
            "",
            "§7⏣ §bVillage",
            "Late Summer 11th, Year 311",
            "§8m77CK",
            "Power: Sighted",
            "",
            "Objective:\n§eUpdate SkyHanni",
            "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills",
            "",
            "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234",
            "§7Wide Range of Events\n(too much for this here)",
            "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff",
            "§9§lParty (4):\n §7- §fhannibal2\n §7- §f Moulberry\n §7- §f Vahvl\n §7- §f J10a1n15",
            "§ewww.hypixel.net",
        }
    )
    public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20, 21, 22));

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
