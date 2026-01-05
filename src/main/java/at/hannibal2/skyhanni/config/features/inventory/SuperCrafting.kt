package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCrafting {
    @Expose
    @Accordion
    @ConfigOption(name = "Super Crafting Coin Waste", desc = "Settings for Super Crafting Coin Waste warnings.")
    val waste = SuperCraftingWasteConfig()
}
