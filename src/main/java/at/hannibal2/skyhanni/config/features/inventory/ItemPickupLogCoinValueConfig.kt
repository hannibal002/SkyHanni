package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ItemPickupLogCoinValueConfig {

    @Expose
    @ConfigOption(name = "Total Coin Value", desc = "Show total coin value of the items in your pickup log.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Price Source", desc = "What price source to use for total coin value.")
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_SELL

    @Expose
    @ConfigOption(name = "Total Coin Value Threshold", desc = "Only show total coin value when above this threshold.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1_000_000f, minStep = 1000f)
    var threshold: Float = 10_000f
}
