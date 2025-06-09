package at.hannibal2.skyhanni.config.features.foraging

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingConfig {

    @Expose
    @ConfigOption(name = "Moonglade Beacon", desc = "Settings for the moonglade beacon.")
    @Accordion
    var moongladeBeacon = MoongladeBeaconConfig()




}
