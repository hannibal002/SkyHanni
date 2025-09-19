package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

open class ItemTrackerGenericConfig : TrackerGenericConfig() {
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
        @ConfigOption(name = "Hide with Item Value", desc = "Hide while the Estimated Item Value is visible.")
        @ConfigEditorBoolean
        var hideInEstimatedItemValue: Boolean = true

        @Expose
        @ConfigOption(name = "Hide outside Inventory", desc = "Hide Profit Trackers while not inside an inventory.")
        @ConfigEditorBoolean
        var hideOutsideInventory: Boolean = false

        @Expose
        @ConfigOption(name = "Item Warnings", desc = "Item Warnings")
        @SearchTag("Tracker Title, Drop Title")
        @Accordion
        val warnings: TrackerItemWarningsConfig = TrackerItemWarningsConfig()

        class TrackerItemWarningsConfig {
            @Expose
            @ConfigOption(
                name = "Price in Chat",
                desc = "Show an extra chat message when you pick up an expensive item. (This contains name, amount and price)"
            )
            @ConfigEditorBoolean
            @FeatureToggle
            var chat: Boolean = true

            @Expose
            @ConfigOption(name = "Minimum Price", desc = "Items below this price will not show up in chat.")
            @ConfigEditorSlider(minValue = 1f, maxValue = 20000000f, minStep = 1f)
            var minimumChat: Int = 5000000

            @Expose
            @ConfigOption(name = "Title Warning", desc = "Show a title for expensive item pickups.")
            @ConfigEditorBoolean
            @FeatureToggle
            var title: Boolean = true

            @Expose
            @ConfigOption(name = "Title Price", desc = "Items above this price will show up as a title.")
            @ConfigEditorSlider(minValue = 1f, maxValue = 50000000f, minStep = 1f)
            var minimumTitle: Int = 5000000

            fun syncSettings() {
                val config = SkyHanniMod.feature.misc.tracker.itemTracker.warnings
                chat = config.chat
                minimumChat = config.minimumChat
                title = config.title
                minimumTitle = config.minimumTitle
            }
        }

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

        fun syncSettings() {
            val config = SkyHanniMod.feature.misc.tracker.itemTracker
            textOrder.get().clear()
            textOrder.get().addAll(config.textOrder.get())
            hideOutsideInventory = config.hideOutsideInventory
            hideInEstimatedItemValue = config.hideInEstimatedItemValue
            profitPerHour.set(config.profitPerHour.get())
            itemsShown.set(config.itemsShown.get())
            showTable.set(config.showTable.get())
            excludeHiddenItemsInPrice = config.excludeHiddenItemsInPrice
            showRecentDrops = config.showRecentDrops
            warnings.syncSettings()
        }

        enum class TextPart(private val displayName: String) {
            ICON("Item Icon"),
            NAME("Item Name"),
            AMOUNT("Amount"),
            TOTAL_PRICE("Total Price"),
            ;

            override fun toString() = displayName
        }
    }
}
