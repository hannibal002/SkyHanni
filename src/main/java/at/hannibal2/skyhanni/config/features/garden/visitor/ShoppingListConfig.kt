package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ShoppingListConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show all items required for the visitors.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = ShoppingListConfig::class, field = "enabled")
    var position: Position = Position(180, 170)

    @Expose
    @ConfigOption(name = "Only when Close", desc = "Only show the shopping list when close to the visitors.")
    @ConfigEditorBoolean
    var onlyWhenClose: Boolean = false

    @Expose
    @ConfigOption(
        name = "Bazaar Alley",
        desc = "Show the Visitor Items List while inside the Bazaar Alley in the Hub.\n" +
            "§eHelps in buying the correct amount when not having a §6Booster Cookie §ebuff active."
    )
    @ConfigEditorBoolean
    var inBazaarAlley: Boolean = true

    @Expose
    @ConfigOption(
        name = "Farming Areas",
        desc = "Show the Visitor Shopping List while on the Farming Islands or inside the Farm in the Hub.\n" +
            "§eHelps in farming the correct amount, especially when in the early game."
    )
    @ConfigEditorBoolean
    var inFarmingAreas: Boolean = false

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show the coin price in the shopping list.")
    @ConfigEditorBoolean
    var showPrice: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Sack Count",
        desc = "Show the amount of this item that you already have in your sacks.\n" +
            "§eOnly updates on sack change messages."
    )
    @ConfigEditorBoolean
    var showSackCount: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Super Craft",
        desc = "Show super craft button if there are enough materials to make in the sack."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showSuperCraft: Boolean = false

    @Expose
    @ConfigOption(
        name = "Item Preview",
        desc = "Show the base type for the required items next to new visitors.\n" +
            "§cNote that some visitors may require any crop."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var itemPreview: Boolean = true

    @Expose
    @ConfigOption(name = "Ignore Spaceman", desc = "Exclude crops requested by Spaceman from the shopping list.")
    @ConfigEditorBoolean
    var ignoreSpaceman: Boolean = false
}
