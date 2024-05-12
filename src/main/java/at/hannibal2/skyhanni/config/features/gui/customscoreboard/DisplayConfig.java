package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class DisplayConfig {

    @Expose
    @ConfigOption(name = "Alignment Options", desc = "")
    @Accordion
    public AlignmentConfig alignment = new AlignmentConfig();

    @Expose
    @ConfigOption(name = "Arrow Options", desc = "")
    @Accordion
    public ArrowConfig arrow = new ArrowConfig();

    @Expose
    @ConfigOption(name = "Events Options", desc = "")
    @Accordion
    public EventsConfig events = new EventsConfig();

    @Expose
    @ConfigOption(name = "Maxwell Options", desc = "")
    @Accordion
    public MaxwellConfig maxwell = new MaxwellConfig();

    @Expose
    @ConfigOption(name = "Mayor Options", desc = "")
    @Accordion
    public MayorConfig mayor = new MayorConfig();

    @Expose
    @ConfigOption(name = "Party Options", desc = "")
    @Accordion
    public PartyConfig party = new PartyConfig();

    @Expose
    @ConfigOption(name = "Title and Footer Options", desc = "")
    @Accordion
    public TitleAndFooterConfig titleAndFooter = new TitleAndFooterConfig();


    @Expose
    @ConfigOption(name = "Hide Vanilla Scoreboard", desc = "Hide the vanilla scoreboard." +
        "\n§cUsing mods that add their own scoreboard will not be affected by this setting!")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> hideVanillaScoreboard = Property.of(true);

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
    @ConfigOption(name = "Show Max Island Players", desc = "Show the maximum amount of players that can join your current island.")
    @ConfigEditorBoolean
    public boolean showMaxIslandPlayers = true;

    @Expose
    @ConfigOption(name = "Powder Display", desc = "Select how the powder display should be formatted.")
    @ConfigEditorDropdown
    public PowderDisplay powderDisplay = PowderDisplay.AVAILABLE;

    public enum PowderDisplay {
        AVAILABLE("Available"),
        TOTAL("Total"),
        BOTH("Available / All")
        ;

        private final String str;

        PowderDisplay(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Number Format", desc = "")
    @ConfigEditorDropdown
    public NumberFormat numberFormat = NumberFormat.LONG;

    public enum NumberFormat {
        LONG("1,234,567"),
        SHORT("1.2M");

        private final String str;

        NumberFormat(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Line Spacing", desc = "The amount of space between each line.")
    @ConfigEditorSlider(minValue = 0, maxValue = 20, minStep = 1)
    public int lineSpacing = 10;

    @Expose
    @ConfigOption(name = "Cache Scoreboard on Island Switch",
        desc = "Will stop the Scoreboard from updating while switching islands.\nRemoves the shaking when loading data.")
    @ConfigEditorBoolean
    public boolean cacheScoreboardOnIslandSwitch = false;
}
