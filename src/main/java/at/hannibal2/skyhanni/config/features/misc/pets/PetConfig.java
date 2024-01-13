package at.hannibal2.skyhanni.config.features.misc.pets;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PetConfig {
    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    public Position displayPos = new Position(-330, -15, false, true);

    @Expose
    @ConfigOption(name = "Pet Experience Tooltip", desc = "")
    @Accordion
    public PetExperienceToolTipConfig petExperienceToolTip = new PetExperienceToolTipConfig();

    @Expose
    @ConfigOption(name = "Hide Autopet Messages", desc = "Hides the autopet messages from chat. §eRequires the " +
        "display to be enabled.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideAutopet = false;
}
