package at.hannibal2.skyhanni.config.features.rift.area.wyldwoods;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class WyldWoodsConfig {

    @Expose
    @ConfigOption(name = "Shy Crux Warning", desc = "Shows a warning when a Shy Crux is going to steal your time. " +
        "Useful if you play without volume.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shyWarning = true;

    @ConfigOption(name = "Larvas", desc = "")
    @Accordion
    @Expose
    public LarvasConfig larvas = new LarvasConfig();

    @ConfigOption(name = "Odonatas", desc = "")
    @Accordion
    @Expose
    public OdonataConfig odonata = new OdonataConfig();
}
