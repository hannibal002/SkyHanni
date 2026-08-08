package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TooltipPriceInfoConfig {

    @Expose
    @ConfigOption(name = "Tooltip Price Info", desc = "")
    @ConfigEditorBoolean
    @FeatureToggle
    var showPriceInLore: Boolean = true

    @Expose
    @ConfigOption(name = "Price Info", desc = "Select what price information you would like to see")
    @ConfigEditorDraggableList
    var priceTypes: MutableList<PriceTypes> = mutableListOf(
        PriceTypes.LBIN,
        PriceTypes.INSTA_BUY,
        PriceTypes.INSTA_SELL,
        PriceTypes.NPC,
        PriceTypes.CRAFT_COST
    )

    enum class PriceTypes(val displayName: String) {
        LBIN("Lowest BIN"),
        //AVG_LBIN_1("Average Lowest BIN (1 day)"),
        //AVG_LBIN_3("Average Lowest BIN (3 days)"),
        //AVG_LBIN_7("Average Lowest BIN (7 days)"),
        INSTA_BUY("Insta Buy"),
        INSTA_SELL("Insta Sell"),
        NPC("NPC Sell Price"),
        CRAFT_COST("Craft Cost");

        override fun toString() = displayName
    }
}
