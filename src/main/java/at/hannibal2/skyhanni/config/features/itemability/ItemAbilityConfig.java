package at.hannibal2.skyhanni.config.features.itemability;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class ItemAbilityConfig {

    @Expose
    @ConfigOption(name = "Ability Cooldown", desc = "Show the cooldown of item abilities.")
    @ConfigEditorBoolean
    @FeatureToggle
    // TODO rename to "enabled"
    public boolean itemAbilityCooldown = false;

    @Expose
    @ConfigOption(name = "Ability Cooldown Background", desc = "Show the cooldown color of item abilities in the background.")
    @ConfigEditorBoolean
    public boolean itemAbilityCooldownBackground = false;

    @Expose
    @ConfigOption(name = "Show When Ready", desc = "Show the R and background (if enabled) when the ability is ready.")
    @ConfigEditorBoolean
    public Property<Boolean> itemAbilityShowWhenReady = Property.of(true);

    @Expose
    @ConfigOption(name = "Fire Veil", desc = "")
    @Accordion
    public FireVeilWandConfig fireVeilWands = new FireVeilWandConfig();

    @ConfigOption(name = "Chicken Head", desc = "")
    @Accordion
    @Expose
    public ChickenHeadConfig chickenHead = new ChickenHeadConfig();

    @Expose
    @ConfigOption(name = "Depleted Bonzo's Masks",
        desc = "Highlights used Bonzo's Masks and Spirit Masks with a background.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean depletedBonzosMasks = false;
}
