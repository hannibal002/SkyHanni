package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.utils.LorenzColor;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ContestSummaryConfig {
    @Expose
    @ConfigOption(
        name = "Ênabled",
        desc = "Show the average Blocks Per Second and blocks clicked at the end of a Jacob Farming Contest in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean jacobContestSummary = true;

    @Expose
    @ConfigOption(name = "Good BPS", desc = "")
    @Accordion
    public GoodBPSConfig good = new GoodBPSConfig();

    @Expose
    @ConfigOption(name = "Okay BPS", desc = "")
    @Accordion
    public OkayBPSConfig okay = new OkayBPSConfig();

    @Expose
    @ConfigOption(name = "Bad BPS", desc = "")
    @Accordion
    public BadBPSConfig bad = new BadBPSConfig();

    public static class GoodBPSConfig {
        @Expose
        @ConfigOption(name = "Color", desc = "Color for good BPS.")
        @ConfigEditorDropdown
        public LorenzColor color = LorenzColor.GREEN;

        @Expose
        @ConfigOption(name = "Threshold", desc = "Threshold for good BPS.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 0.5f)
        public float threshold = 19f;
    }

    public static class OkayBPSConfig {
        @Expose
        @ConfigOption(name = "Color", desc = "Color for okay BPS.")
        @ConfigEditorDropdown
        public LorenzColor color = LorenzColor.YELLOW;

        @Expose
        @ConfigOption(name = "Threshold", desc = "Threshold for okay BPS.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 0.5f)
        public float threshold = 15f;
    }

    public static class BadBPSConfig {
        @Expose
        @ConfigOption(name = "Color", desc = "Color for bad BPS.")
        @ConfigEditorDropdown
        public LorenzColor color = LorenzColor.RED;

        @Expose
        @ConfigOption(name = "Threshold", desc = "Anything below the threshold of okay BPS.")
        @ConfigEditorInfoText()
        public boolean thresholdInformation = false;
    }
}
