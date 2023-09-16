package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CrimsonIsleConfig {

    @ConfigOption(name = "Ashfang", desc = "")
    @Accordion
    @Expose
    public AshfangConfig ashfang = new AshfangConfig();

    public static class AshfangConfig {

        @ConfigOption(name = "Gravity Orbs", desc = "")
        @Accordion
        @Expose
        public GravityOrbsConfig gravityOrbs = new GravityOrbsConfig();

        public static class GravityOrbsConfig {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Shows the Gravity Orbs more clearly.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Color", desc = "Color of the Gravity Orbs.")
            @ConfigEditorColour
            public String color = "0:120:255:85:85";
        }
    }
}
