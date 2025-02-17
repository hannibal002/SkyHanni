package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FrogMaskFeaturesConfig {
    @Expose
    @ConfigOption(name = "Frog Mask Display", desc = "Displays information about the §5Frog Mask§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean frogMaskDisplay = false;

    @Expose
    @ConfigOption(name = "Frog Mask Warning", desc = "Displays a warning when foraging in the wrong region of the park while wearing a §5Frog Mask§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean frogMaskWarning = false;

    @Expose
    @ConfigLink(owner = MiscConfig.class, field = "frogMaskDisplay")
    public Position frogMaskDisplayPosition = new Position(25, 25, false, true);
}
