package at.hannibal2.skyhanni.config.features.rift.area.mountaintop

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MountaintopConfig {

    @Expose
    @ConfigOption(name = "Sun Gecko", desc = "")
    @Accordion
    val sunGecko: SunGeckoConfig = SunGeckoConfig()

    @Expose
    @ConfigOption(name = "Timite", desc = "")
    @Accordion
    val timite: TimiteConfig = TimiteConfig()

    @Expose
    @ConfigOption(
        name = "Enigma Rose' End Flowerpot",
        desc = "Show the dropdown location to the hard Flowerpot point while in the Enigma Rose' End quest.",
    )
    @ConfigEditorBoolean
    var enigmaRoseFlowerpot: Boolean = true

    @Expose
    @ConfigOption(name = "Ubik's Cube Reminder", desc = "Reminder when the 2 hours are over for Ubik's Cube in the Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ubikReminder: Boolean = false
}
