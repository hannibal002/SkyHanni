package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ItemAbilityConfig {

    @Expose
    @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean itemAbilityCooldown = false;

    @Expose
    @ConfigOption(name = "Ability Cooldown Background", desc = "Show the cooldown color of item abilities in the background.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldownBackground = false;

    @Expose
    @ConfigOption(name = "Fire Veil", desc = "")
    @Accordion
    public FireVeilWandConfig fireVeilWands = new FireVeilWandConfig();

    public static class FireVeilWandConfig {
        @Expose
        @ConfigOption(name = "Fire Veil Design", desc = "Changes the flame particles of the Fire Veil Wand ability.")
        @ConfigEditorDropdown(values = {"Particles", "Line", "Off"})
        public int display = 0;

        @Expose
        @ConfigOption(
                name = "Line Color",
                desc = "Changes the color of the Fire Veil Wand line."
        )
        @ConfigEditorColour
        public String displayColor = "0:245:255:85:85";
    }

    @ConfigOption(name = "Chicken Head", desc = "")
    @Accordion
    @Expose
    public ChickenHeadConfig chickenHead = new ChickenHeadConfig();

    public static class ChickenHeadConfig {

        @Expose
        @ConfigOption(name = "Checken Head Timer", desc = "Show the cooldown until the next time you can lay an egg with the Chicken Head.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean displayTimer = false;

        @Expose
        public Position position = new Position(-372, 73, false, true);

        @Expose
        @ConfigOption(name = "Hide Chat", desc = "Hide the 'You laid an egg!' chat message.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideChat = true;
    }

    @Expose
    @ConfigOption(name = "Depleted Bonzo's Masks",
            desc = "Highlights used Bonzo's Masks and Spirit Masks with a background.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean depletedBonzosMasks = false;
}
