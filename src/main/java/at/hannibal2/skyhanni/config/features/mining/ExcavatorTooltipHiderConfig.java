package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ExcavatorTooltipHiderConfig {

    @Expose
    @ConfigOption(name = "Hide Excavator Tooltips", desc = "Hides tooltips of the Dirt inside of the Fossil Excavator.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideExcavatorTooltips = true;

    @Expose
    @ConfigOption(name = "Hide all Excavator Tooltips", desc = "Hide all tooltips inside inside of the Fossil Excavator.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideAllExcavatorTooltips = false;
}
