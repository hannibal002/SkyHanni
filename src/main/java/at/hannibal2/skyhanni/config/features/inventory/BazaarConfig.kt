package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BazaarConfig {
    @Expose
    @ConfigOption(name = "Purchase Helper", desc = "Highlights the item you are trying to buy in the Bazaar.")
    @ConfigEditorBoolean
    @FeatureToggle
    var purchaseHelper: Boolean = true

    @Expose
    @ConfigOption(
        name = "Order Helper",
        desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var orderHelper: Boolean = false

    @Expose
    @ConfigOption(name = "Best Sell Method", desc = "Show the price difference between sell instantly and sell offer.")
    @ConfigEditorBoolean
    @FeatureToggle
    var bestSellMethod: Boolean = false

    // TODO rename to bestSellMethodPosition
    @Expose
    @ConfigLink(owner = BazaarConfig::class, field = "bestSellMethod")
    val bestSellMethodPos: Position = Position(394, 142)

    @Expose
    @ConfigOption(
        name = "Cancelled Buy Order Clipboard",
        desc = "Send missing items from cancelled buy orders in chat.\n" +
            "Click on the message to quickly order the same item and amount again."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var cancelledBuyOrderClipboard: Boolean = false

    @Expose
    @ConfigOption(
        name = "Price Website",
        desc = "Add a button to the Bazaar product inventory that will open the item page in §cskyblock.bz§7."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var openPriceWebsite: Boolean = false

    @Expose
    @ConfigOption(
        name = "Max Items With Purse",
        desc = "Calculate the maximum amount of items that can be purchased from the Bazaar with the amount of coins in your purse."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var maxPurseItems: Boolean = false

    @Expose
    @ConfigLink(owner = BazaarConfig::class, field = "maxPurseItems")
    val maxPurseItemsPosition: Position = Position(346, 90, centerX = true, centerY = false)

    // TODO rename and move (idk where or what, hanni TODO)
    @Expose
    @ConfigOption(
        name = "Craft Materials Bazaar",
        desc = "In the crafting view, offer a shopping list of required materials for the craft" +
            " along with a shortcut for purchasing them from the Bazaar or Auction House."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var craftMaterialsFromBazaar: Boolean = false

    @Expose
    @ConfigLink(owner = BazaarConfig::class, field = "craftMaterialsFromBazaar")
    val craftMaterialsFromBazaarPosition: Position = Position(50, 50, centerX = true, centerY = false)
}
