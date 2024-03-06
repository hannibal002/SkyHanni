package at.hannibal2.skyhanni.config.features.rift.area.livingcave;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class LivingCaveLivingMetalConfig {

    @Expose
    @ConfigOption(name = "Living Metal", desc = "Show a moving animation between Living Metal and the next block.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide Living Metal particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideParticles = false;

}
