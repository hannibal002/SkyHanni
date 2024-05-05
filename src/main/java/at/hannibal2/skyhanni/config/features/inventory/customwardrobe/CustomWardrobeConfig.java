package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CustomWardrobeConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enables the Custom Wardrobe GUI.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Follow mouse", desc = "Players follow the movement of the mouse.")
    @ConfigEditorBoolean
    public boolean eyesFollowMouse = true;

    @Expose
    @ConfigOption(name = "Hide Empty Slots", desc = "Hides wardrobe slots with no armor.")
    @ConfigEditorBoolean
    public boolean hideEmptySlots = false;

    @Expose
    @ConfigOption(name = "Hide Locked Slots", desc = "Hides locked wardrobe slots.")
    @ConfigEditorBoolean
    public boolean hideLockedSlots = false;

    public boolean onlyFavorites = false;

    @Expose
    @ConfigOption(name = "Estimated Value", desc = "Show a §2$ §7sign you can hover to see the wardrobe slot value.")
    @ConfigEditorBoolean
    public boolean estimatedValue = true;

    @Expose
    @ConfigOption(name = "Colors", desc = "Change the color settings.")
    @Accordion
    public ColorConfig color = new ColorConfig();

    @Expose
    @ConfigOption(name = "Spacing", desc = "")
    @Accordion
    public SpacingConfig spacing = new SpacingConfig();
}
