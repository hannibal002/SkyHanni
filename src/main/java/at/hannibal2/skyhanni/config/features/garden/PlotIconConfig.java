package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.features.garden.inventory.GardenPlotIcon;
import at.hannibal2.skyhanni.utils.LorenzUtils;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PlotIconConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Enable icon replacement in the Configure Plots menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @ConfigOption(name = "Hard Reset", desc = "Reset every slot to its original item.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable hardReset = () -> {
        GardenPlotIcon.INSTANCE.setHardReset(true);
        LorenzUtils.INSTANCE.sendCommandToServer("desk");
    };
}
