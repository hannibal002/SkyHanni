package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.inventory.ItemPickupLog
import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ItemPickupLogConfig {
    @Expose
    @ConfigOption(name = "Item Pickup Log", desc = "Show a log of what items you pick up/drop and their amounts.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Compact Lines", desc = "Combine the §a+ §7and §c- §7lines into a single line.")
    @ConfigEditorBoolean
    var compactLines: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Numbers", desc = "Compact the amounts added and removed.")
    @ConfigEditorBoolean
    var shorten: Boolean = false

    @Expose
    @ConfigOption(name = "Sacks", desc = "Show items added and removed from sacks.")
    @ConfigEditorBoolean
    var sack: Boolean = false

    @Expose
    @ConfigOption(name = "Shards", desc = "Show items added and removed from your hunting box.")
    @ConfigEditorBoolean
    var shards: Boolean = false

    @Expose
    @ConfigOption(name = "Coins", desc = "Show coins added and removed from purse.")
    @ConfigEditorBoolean
    var coins: Boolean = false

    @Expose
    @ConfigOption(name = "Alignment", desc = "How the item pickup log should be aligned.")
    @ConfigEditorDropdown
    var alignment: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP

    @Expose
    @ConfigOption(name = "Layout", desc = "Drag text to change the layout. List will be rendered horizontally")
    @ConfigEditorDraggableList(requireNonEmpty = true)
    val displayLayout: MutableList<ItemPickupLog.DisplayLayout> = mutableListOf(
        ItemPickupLog.DisplayLayout.CHANGE_AMOUNT,
        ItemPickupLog.DisplayLayout.ICON,
        ItemPickupLog.DisplayLayout.ITEM_NAME
    )

    @Expose
    @ConfigOption(name = "Expire After", desc = "How long items show for after being picked up or dropped, in seconds.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var expireAfter: Int = 10

    @Expose
    @ConfigLink(owner = ItemPickupLogConfig::class, field = "enabled")
    val position: Position = Position(-256, 140)
}


