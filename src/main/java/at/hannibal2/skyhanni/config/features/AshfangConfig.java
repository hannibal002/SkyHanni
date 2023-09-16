package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AshfangConfig {

    @Expose
    @ConfigOption(name = "Freeze", desc = "Show the cooldown for how long Ashfang blocks your abilities.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean freezeCooldown = false;

    @Expose
    public Position freezeCooldownPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Reset Time", desc = "Show the cooldown until Ashfang pulls his underlings back.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean nextResetCooldown = false;

    @Expose
    public Position nextResetCooldownPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Blazing Souls", desc = "Shows the Blazing Souls more clearly.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean blazingSouls = false;

    @Expose
    @ConfigOption(name = "Souls Color", desc = "Color of the Ashfang Blazing Souls.")
    @ConfigEditorColour
    public String blazingSoulsColor = "0:245:85:255:85";

    @Expose
    @ConfigOption(name = "Highlight Blazes", desc = "Highlight the different blazes in their respective colors.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightBlazes = false;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around the Ashfang boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideParticles = false;

    @Expose
    @ConfigOption(name = "Hide Names", desc = "Hide the names of full health blazes around Ashfang (only useful when highlight blazes is enabled)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideNames = false;

    @Expose
    @ConfigOption(name = "Hide Damage", desc = "Hide damage splashes around Ashfang.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideDamageSplash = false;
}
