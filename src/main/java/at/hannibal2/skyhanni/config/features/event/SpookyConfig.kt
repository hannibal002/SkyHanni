package at.hannibal2.skyhanni.config.features.event

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpookyConfig {

    @Accordion
    @ConfigOption(name = "Spooky", desc = "Spooky Festival")
    @Expose
    val spookyChests: SpookyChestConfig = SpookyChestConfig()

}
