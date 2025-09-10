package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DefaultDisplayMode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

open class TrackerGenericConfig {
    private val config get() = SkyHanniMod.feature.misc

    @Expose
    @ConfigOption(name = "Default Display Mode", desc = "Change the display mode that gets shown on default.")
    @ConfigEditorDropdown
    val defaultDisplayMode: Property<DefaultDisplayMode> = Property.of(DefaultDisplayMode.REMEMBER_LAST)

    @Expose
    @ConfigOption(name = "Show Uptime", desc = "Show how long the tracker has been active.")
    @ConfigEditorBoolean
    val showUptime: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Only Show Session Uptime",
        desc = "Only show uptime and profit per hour when the tracker is on session mode."
    )
    @ConfigEditorBoolean
    val onlyShowSession: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "AFK timeout",
        desc = "Pause the tracker if it is not modified for this amount of seconds."
    )
    @ConfigEditorSlider(minValue = 15f, maxValue = 900f, minStep = 15f)
    var afkTimeout: Int = 300

    @Expose
    @ConfigOption(name = "Tracker Search", desc = "Add a search bar to tracker GUIs.")
    @ConfigEditorBoolean
    val trackerSearchEnabled: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY

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
    }


    private fun syncGenericSettings() {
        priceSource = config.tracker.priceSource
        onlyShowSession.set(config.tracker.onlyShowSession.get())
        afkTimeout = config.tracker.afkTimeout
        onlyShowSession.set(config.tracker.onlyShowSession.get())
        showUptime.set(config.tracker.showUptime.get())
        defaultDisplayMode.set(config.tracker.defaultDisplayMode.get())
    }

    open fun syncSettings() {
        syncGenericSettings()
    }
}
