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
        name = "Appearance",
        desc = "Drag text to change the appearance of the advanced scoreboard." // now supporting both custom & advanced search
    )
    @ConfigEditorDraggableList()
    public List<ScoreboardEntry> textFormat = new ArrayList<>(Arrays.asList(ScoreboardEntry.values()));

    public enum ScoreboardEntry {
        TITLE("§6§lSKYBLOCK"),
        PROFILE("§7♲ Blueberry"),
        PURSE("Purse: §652,763,737"),
        MOTES("Motes: §d64,647"),
        BANK("Bank: §6249M"),
        BITS("Bits: §b59,264"),
        COPPER("Copper: §c23,495"),
        GEMS("Gems: §a57,873"),
        HEAT("Heat: §c♨ 0"),
        EMPTY_LINE(""),
        LOCATION("§7⏣ §bVillage"),
        DATE("Late Summer 11th"),
        TIME("§710:40pm"),
        LOBBY_CODE("§8m77CK"),
        POWER("Power: Sighted"),
        EMPTY_LINE2(""),
        OBJECTIVE("Objective:\n§eUpdate SkyHanni"),
        SLAYER("§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"),
        EMPTY_LINE3(""),
        POWDER("§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234"),
        EVENTS("§7Wide Range of Events\n§7(too much for this here)"),
        MAYOR("§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"),
        PARTY("§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fJ10a1n15"),
        FOOTER("§ewww.hypixel.net"),
        EXTRA("§7Extra lines the mod is not detecting")
        ;

        private final String str;

        ScoreboardEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
