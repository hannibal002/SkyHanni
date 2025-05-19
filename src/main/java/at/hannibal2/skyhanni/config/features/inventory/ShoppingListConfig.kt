package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ShoppingListConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Generall shopping list")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = ShoppingListConfig::class, field = "enabled")
    var position: Position = Position(10, 10, false, false)

    @Expose
    @ConfigOption(name = "Show overall", desc = "For each item, show how many items are needed over the whole shopping list")
    @ConfigEditorBoolean
    var showOverall: Boolean = true

    @Expose
    @ConfigOption(name = "Show the cost of each item", desc = "Show the cost of each item in the shopping list")
    @ConfigEditorBoolean
    var showItemCost: Boolean = true

    @Expose
    @ConfigOption(name = "Show total cost", desc = "Show the total cost of all items in the shopping list")
    @ConfigEditorBoolean
    var showTotalCost: Boolean = true
}
