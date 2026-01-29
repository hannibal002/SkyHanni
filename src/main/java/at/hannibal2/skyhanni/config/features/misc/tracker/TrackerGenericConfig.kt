package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DefaultDisplayMode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

open class TrackerGenericConfig {
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
    var afkTimeout: Int = 60

    @Expose
    @ConfigOption(name = "Tracker Search", desc = "Add a search bar to tracker GUIs.")
    @SearchTag("uptime")
    @ConfigEditorBoolean
    val trackerSearchEnabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC."
    )
    @ConfigEditorDropdown
    var priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY

    private fun syncGenericSettings() {
        val config = SkyHanniMod.feature.misc
        onlyShowSession.set(config.tracker.onlyShowSession.get())
        afkTimeout = config.tracker.afkTimeout
        showUptime.set(config.tracker.showUptime.get())
        defaultDisplayMode.set(config.tracker.defaultDisplayMode.get())
        trackerSearchEnabled.set(config.tracker.trackerSearchEnabled.get())
        priceSource = config.tracker.priceSource
    }

    open fun syncSettings() {
        syncGenericSettings()
    }
}
