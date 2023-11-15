package at.hannibal2.skyhanni.config.features.rift.area.stillgorechateau;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class StillgoreChateauConfig {

    @Expose
    @ConfigOption(name = "Blood Effigies", desc = "")
    @Accordion
    public EffigiesConfig bloodEffigies = new EffigiesConfig();

}
