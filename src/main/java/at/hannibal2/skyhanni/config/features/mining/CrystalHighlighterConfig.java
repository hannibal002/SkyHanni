package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CrystalHighlighterConfig {

    @Expose
    @ConfigOption(
        name = "Highlight Crystal Nucleus barriers",
        desc = "Draw visible bounding boxes around the Crystal Nucleus crystal barrier blocks."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Display Type",
        desc = "Choose the display type for the highlighted boxes."
    )
    public DisplayType displayType = DisplayType.CRYSTAL_COLORS;

    public enum DisplayType {
        CRYSTAL_COLORS("Crystal Colors"),
        CHROMA("Chroma"),
        ;

        private final String str;

        DisplayType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "Highlight Opacity",
        desc = "Set the opacity of the highlighted boxes.\n§80§7: §8Transparent\n§81§7: §fSolid"
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
    public float opacity = 0.6f;

    @Expose
    @ConfigOption(
        name = "Only Show During Hoppity's",
        desc = "Only show the highlighted boxes during Hoppity's Hunt."
    )
    @ConfigEditorBoolean
    public boolean onlyDuringHoppity = false;
}
