package at.hannibal2.skyhanni.config.features.misc.frogmask;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import scala.collection.mutable.MutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FrogMaskWarningConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Displays a warning when foraging/being in a wrong region of the park while wearing a §5Frog Mask§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Cooldown", desc = "Change how much time needs to pass before you get warned again.")
    @ConfigEditorSlider(minValue = 5, maxValue = 60, minStep = 1)
    public int cooldown = 30;

    @Expose
    @ConfigOption(name = "Warning Type", desc = "Change when you want to be warned.")
    @ConfigEditorDraggableList
    public List<WarningType> warningTypes = new ArrayList<>(Collections.singletonList(WarningType.FORAGING));

    public enum WarningType {
        BEING("When being in a wrong region."),
        FORAGING("When foraging in a wrong region.");

        private final String displayName;

        WarningType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
