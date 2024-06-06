package at.hannibal2.skyhanni.config.features.rift.area.stillgorechateau;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class StillgoreChateauConfig {

    @Expose
    @ConfigOption(name = "Blood Effigies", desc = "")
    @Accordion
    public EffigiesConfig bloodEffigies = new EffigiesConfig();

}
