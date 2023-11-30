package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.misc.compacttablist.AdvancedPlayerListConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
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
        desc = "Show a custom scoreboard instead of the vanilla one."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the scoreboard."
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
            "Late Summer 11th",
            "§710:40pm",
            "§8m77CK",
            "Power: Sighted",
            "",
            "Objective:\n§eUpdate SkyHanni",
            "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills",
            "",
            "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234",
            "§7Wide Range of Events\n§7(too much for this here)",
            "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff",
            "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fJ10a1n15",
            "§ewww.hypixel.net",
        }
    )
    public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23));

    @Expose
    @ConfigOption(name = "Display Options", desc = "")
    @Accordion
    public DisplayConfig displayConfig = new DisplayConfig();

    @Expose
    @ConfigOption(name = "Information Filtering", desc = "")
    @Accordion
    public InformationFilteringConfig informationFilteringConfig = new InformationFilteringConfig();

    @Expose
    @ConfigOption(name = "Background Options", desc = "")
    @Accordion
    public BackgroundConfig backgroundConfig = new BackgroundConfig();

    @Expose
    @ConfigOption(name = "Party Options", desc = "")
    @Accordion
    public PartyConfig partyConfig = new PartyConfig();

    @Expose
    @ConfigOption(name = "Show Mayor Perks", desc = "Show the perks of the current mayor.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showMayorPerks = true;

    @Expose
    public Position position = new Position(10, 80, false, true);
}
