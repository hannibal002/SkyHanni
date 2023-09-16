package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
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

        @ConfigOption(name = "Blazing Souls", desc = "")
        @Accordion
        @Expose
        public BlazingSoulsColor blazingSouls = new BlazingSoulsColor();

        public static class BlazingSoulsColor {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Shows the Blazing Souls more clearly.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Souls Color", desc = "Color of the Blazing Souls.")
            @ConfigEditorColour
            public String color = "0:245:85:255:85";
        }

        @ConfigOption(name = "Hide Stuff", desc = "")
        @Accordion
        @Expose
        public HideAshfangConfig hide = new HideAshfangConfig();

        public static class HideAshfangConfig {

            @Expose
            @ConfigOption(name = "Hide Particles", desc = "Hide particles around the Ashfang boss.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean particles = false;

            @Expose
            @ConfigOption(name = "Hide Full Names", desc = "Hide the names of full health blazes around Ashfang (only useful when highlight blazes is enabled)")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean fullNames = false;

            @Expose
            @ConfigOption(name = "Hide Damage Splash", desc = "Hide damage splashes around Ashfang.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean damageSplash = false;
        }

        @Expose
        @ConfigOption(name = "Highlight Blazes", desc = "Highlight the different blazes in their respective colors.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightBlazes = false;

        @Expose
        @ConfigOption(name = "Freeze Cooldown", desc = "Show the cooldown for how long Ashfang blocks your abilities.")
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
    }
}
