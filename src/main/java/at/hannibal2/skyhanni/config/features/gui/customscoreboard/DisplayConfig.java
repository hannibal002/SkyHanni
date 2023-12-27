package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DisplayConfig {
    @Expose
    @ConfigOption(name = "Hide Vanilla Scoreboard", desc = "Hide the vanilla scoreboard.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideVanillaScoreboard = false;

    @Expose
    @ConfigOption(name = "Display Numbers First", desc = "Determines whether the number or line name displays first. " +
        "§eNote: Will not update the preview above!")
    @ConfigEditorBoolean
    public boolean displayNumbersFirst = false;

    @Expose
    @ConfigOption(name = "Show unclaimed bits", desc = "Show the amount of available Bits that can still be claimed.")
    @ConfigEditorBoolean
    public boolean showUnclaimedBits = false;

    @Expose
    @ConfigOption(name = "Show all active events", desc = "Show all active events in the scoreboard instead of one.")
    @ConfigEditorBoolean
    public boolean showAllActiveEvents = false;

    @Expose
    @ConfigOption(name = "Alignment Options", desc = "")
    @Accordion
    public AlignmentConfig alignment = new AlignmentConfig();

    @Expose
    @ConfigOption(name = "Title and Footer Options", desc = "")
    @Accordion
    public TitleAndFooterConfig titleAndFooter = new TitleAndFooterConfig();
}
