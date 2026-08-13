package at.hannibal2.skyhanni.config.features.inventory.npctrade

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NpcTradeConfig {

    @Expose
    @ConfigOption(
        name = "Highlight Affordable",
        desc = "Highlight items in NPC trade menus that you can buy right now.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightAffordable: Boolean = true

    @Expose
    @ConfigOption(
        name = "Cost Breakdown",
        desc = "Show the price, how much you own and the total cost in the item lore.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var costBreakdown: Boolean = true
}
