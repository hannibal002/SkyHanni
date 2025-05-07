package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
//#if MC < 1.21
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui;
//#endif
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

// todo 1.21 impl needed
public class FarmingFortuneConfig {
    @Expose
    @ConfigOption(
        name = "FF Display",
        desc = "Display the true Farming Fortune for the current crop, including all crop-specific and hidden bonuses."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean display = false;

    @Expose
    @ConfigOption(name = "Compact Format", desc = "Compact the farming fortune display.")
    @ConfigEditorBoolean
    public boolean compactFormat = false;

    @Expose
    @ConfigOption(name = "Hide Missing Fortune Warnings", desc = "Hide missing fortune warnings from the display.")
    @ConfigEditorBoolean
    public boolean hideMissingFortuneWarnings = false;

    //#if MC < 1.21
    @ConfigOption(name = "Farming Fortune Guide", desc = "Open a guide that breaks down your Farming Fortune.\n§eCommand: /ff")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable open = FFGuideGui::onCommand;
    //#endif



    @Expose
    @ConfigLink(owner = FarmingFortuneConfig.class, field = "display")
    public Position pos = new Position(5, -180);
}
