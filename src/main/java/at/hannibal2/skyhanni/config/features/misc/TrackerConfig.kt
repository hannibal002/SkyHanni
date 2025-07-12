package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DefaultDisplayMode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class TrackerConfig {
    @Expose
    @ConfigOption(name = "Hide with Item Value", desc = "Hide all trackers while the Estimated Item Value is visible.")
    @ConfigEditorBoolean
    var hideInEstimatedItemValue: Boolean = true

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY

    @Expose
    @ConfigOption(name = "Default Display Mode", desc = "Change the display mode that gets shown on default.")
    @ConfigEditorDropdown
    val defaultDisplayMode: Property<DefaultDisplayMode> = Property.of(DefaultDisplayMode.REMEMBER_LAST)

    @Expose
    @ConfigOption(name = "Recent Drops", desc = "Highlight the amount in green on recently gained items.")
    @ConfigEditorBoolean
    var showRecentDrops: Boolean = true

    @Expose
    @ConfigOption(name = "Exclude Hidden", desc = "Exclude hidden items in the total price calculation.")
    @ConfigEditorBoolean
    var excludeHiddenItemsInPrice: Boolean = false

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
    }

    @Expose
    @ConfigOption(name = "Show as Table", desc = "Show the list of items as a table.")
    @ConfigEditorBoolean
    val showTable: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Items Shown", desc = "Change the number of item lines shown at once.")
    @ConfigEditorSlider(minValue = 3f, maxValue = 30f, minStep = 1f)
    val itemsShown: Property<Int> = Property.of(10)

    @Expose
    @ConfigOption(name = "Hide outside Inventory", desc = "Hide Profit Trackers while not inside an inventory.")
    @ConfigEditorBoolean
    var hideOutsideInventory: Boolean = false

    @Expose
    @ConfigOption(name = "Tracker Search", desc = "Add a search bar to tracker GUIs.")
    @ConfigEditorBoolean
    val trackerSearchEnabled: Property<Boolean> = Property.of(true)

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

    // Doing this here since SkyHanniTracker isn't a SkyHanniModule
    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(95, "misc.tracker.hideItemTrackersOutsideInventory", "misc.tracker.hideOutsideInventory")
        }
    }
}
