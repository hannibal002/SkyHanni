package at.hannibal2.skyhanni.config.features.rift.area.wyldwoods

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WyldWoodsConfig {
    @Expose
    @ConfigOption(
        name = "Shy Crux Warning",
        desc = "Show a warning when a Shy Crux is going to steal your time. " +
            "Useful if you play without volume."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shyWarning: Boolean = true

    @ConfigOption(name = "Larvas", desc = "")
    @Accordion
    @Expose
    val larvas: LarvasConfig = LarvasConfig()

    @ConfigOption(name = "Odonatas", desc = "")
    @Accordion
    @Expose
    val odonata: OdonataConfig = OdonataConfig()
}
