package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.misc.frogmask.FrogMaskWarningConfig;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FrogMaskFeaturesConfig {
    @Expose
    @ConfigOption(name = "Frog Mask Display", desc = "Displays information about the §5Frog Mask§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    @ConfigOption(name = "Frog Mask Warning", desc = "")
    @Accordion
    public FrogMaskWarningConfig warning = new FrogMaskWarningConfig();

    @Expose
    @ConfigLink(owner = FrogMaskFeaturesConfig.class, field = "display")
    public Position displayPosition = new Position(25, 25, false, true);
}
