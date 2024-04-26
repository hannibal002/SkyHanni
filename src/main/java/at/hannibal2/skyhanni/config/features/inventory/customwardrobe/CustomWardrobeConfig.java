package at.hannibal2.skyhanni.config.features.inventory.customwardrobe;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CustomWardrobeConfig {

    @Expose
    @ConfigOption(name = "enble", desc = "")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "eyes look mose", desc = "")
    @ConfigEditorBoolean
    public boolean eyesFollowMouse = true;

    @Expose
    @ConfigOption(name = "no emty", desc = "")
    @ConfigEditorBoolean
    public boolean hideEmptySlots = false;

    @Expose
    @ConfigOption(name = "colr", desc = "")
    @Accordion
    public ColorConfig color = new ColorConfig();
}
