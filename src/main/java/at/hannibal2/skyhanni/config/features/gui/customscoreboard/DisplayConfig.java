package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

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
    @FeatureToggle
    public boolean displayNumbersFirst = false;

    @Expose
    @ConfigOption(name = "Align to the right", desc = "Align the scoreboard to the right side of the screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean alignRight = false;

    @Expose
    @ConfigOption(name = "Align to the center vertically", desc = "Align the scoreboard to the center of the screen vertically.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean alignCenterVertically = false;

    @Expose
    @ConfigOption(name = "Center Title and Footer", desc = "Center the title and footer to the scoreboard width.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean centerTitleAndFooter = false;

    @Expose
    @ConfigOption(name = "Custom Title", desc = "What should be displayed as the title of the scoreboard.\nUse & for colors.")
    @ConfigEditorText
    public Property<String> customTitle = Property.of("&6&lSKYBLOCK");

    @Expose
    @ConfigOption(name = "Use Hypixel's Title Animation", desc = "Will overwrite the custom title with Hypixel's title animation.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean useHypixelTitleAnimation = false;

    @Expose
    @ConfigOption(name = "Custom Footer", desc = "What should be displayed as the footer of the scoreboard.\nUse & for colors.")
    @ConfigEditorText
    public Property<String> customFooter = Property.of("&ewww.hypixel.net");
}
