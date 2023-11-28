package at.hannibal2.skyhanni.config.features.slayer.endermen;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EndermanConfig {
    @Expose
    @ConfigOption(name = "Yang Glyph (Beacon)", desc = "")
    @Accordion
    public EndermanBeaconConfig beacon = new EndermanBeaconConfig();

    @Expose
    @ConfigOption(name = "Highlight Nukekubi Skulls", desc = "Highlights the Enderman Slayer Nukekubi Skulls (Eyes).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightNukekebi = false;

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Enderman Slayer in damage indicator.")
    @ConfigEditorBoolean
    public boolean phaseDisplay = false;

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide particles around Enderman Slayer bosses and Mini-Bosses.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideParticles = false;
}
