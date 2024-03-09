package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TotemOfCorruptionConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the Totem of Corruption overlay.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showOverlay = true;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide the particles of the Totem of Corruption.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideParticles = true;

    @Expose
    public Position position = new Position(50, 20, false, true);
}
