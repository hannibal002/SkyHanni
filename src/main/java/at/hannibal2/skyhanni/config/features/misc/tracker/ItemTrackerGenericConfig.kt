package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ItemPriceSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

open class ItemTrackerGenericConfig: TrackerGenericConfig() {
    @Expose
    @ConfigOption(
        name = "Item Tracker Settings",
        desc = ""
    )
    @Accordion
    val itemTracker: ItemTrackerConfig = ItemTrackerConfig()

    override fun syncSettings() {
        super.syncSettings()
        itemTracker.syncSettings()
    }

    private val config get() = SkyHanniMod.feature.misc.tracker.itemTracker

    class ItemTrackerConfig {
        @Expose
        @ConfigOption(name = "Recent Drops", desc = "Highlight the amount in green on recently gained items.")
        @ConfigEditorBoolean
        var showRecentDrops: Boolean = true

        @Expose
        @ConfigOption(name = "Exclude Hidden", desc = "Exclude hidden items in the total price calculation.")
        @ConfigEditorBoolean
        var excludeHiddenItemsInPrice: Boolean = false

        @Expose
        @ConfigOption(name = "Show as Table", desc = "Show the list of items as a table.")
        @ConfigEditorBoolean
        val showTable: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Items Shown", desc = "Change the number of item lines shown at once.")
        @ConfigEditorSlider(minValue = 3f, maxValue = 30f, minStep = 1f)
        val itemsShown: Property<Int> = Property.of(10)

        @Expose
        @ConfigOption(name = "Show Profit Per Hour", desc = "Show profit per hour on trackers that show profit.")
        @ConfigEditorBoolean
        val profitPerHour: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(name = "Text Order", desc = "Drag text to change the line format.")
        @ConfigEditorDraggableList
        val textOrder: Property<MutableList<TextPart>> = Property.of(
            mutableListOf(
                TextPart.AMOUNT,
                TextPart.NAME,
                TextPart.TOTAL_PRICE
            )
        )

        enum class TextPart(private val displayName: String) {
            ICON("Item Icon"),
            NAME("Item Name"),
            AMOUNT("Amount"),
            TOTAL_PRICE("Total Price"),
            ;

            override fun toString() = displayName
        }

        fun syncSettings() {
            textOrder.set(config.textOrder.get())
            profitPerHour.set(config.profitPerHour.get())
            hideOutsideInventory = config.hideOutsideInventory
            itemsShown.set(config.itemsShown.get())
            showRecentDrops = config.showRecentDrops
            showTable.set(config.showTable.get())
            excludeHiddenItemsInPrice = config.excludeHiddenItemsInPrice
            hideInEstimatedItemValue = config.hideInEstimatedItemValue
        }
    }
}
