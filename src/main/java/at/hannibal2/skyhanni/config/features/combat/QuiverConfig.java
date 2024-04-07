package at.hannibal2.skyhanni.config.features.combat;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class QuiverConfig {
    @Expose
    @ConfigOption(name = "Quiver Display", desc = "")
    @Accordion
    public QuiverDisplayConfig quiverDisplay = new QuiverDisplayConfig();

    @Expose
    @ConfigOption(name = "Quiver Warning", desc = "")
    @Accordion
    public QuiverWarningConfig quiverWarning = new QuiverWarningConfig();
}
