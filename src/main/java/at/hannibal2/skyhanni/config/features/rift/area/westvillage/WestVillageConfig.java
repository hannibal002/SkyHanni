package at.hannibal2.skyhanni.config.features.rift.area.westvillage;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class WestVillageConfig {

    @ConfigOption(name = "Kloon Hacking", desc = "")
    @Accordion
    @Expose
    public KloonHackingConfig hacking = new KloonHackingConfig();
}
