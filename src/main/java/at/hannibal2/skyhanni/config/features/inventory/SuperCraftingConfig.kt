package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingConfig {

    @Expose
    @Accordion
    @ConfigOption(name = "Waste Warning", desc = "")
    var waste = SuperCraftingWasteConfig()
}
