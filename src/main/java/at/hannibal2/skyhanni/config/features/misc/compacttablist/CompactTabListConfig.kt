package at.hannibal2.skyhanni.config.features.misc.compacttablist

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class CompactTabListConfig {
    @Expose
    @SearchTag("tablist")
    @ConfigOption(
        name = "Enabled",
        desc = "Compact the tab list to make it look much nicer like SBA did. Also " +
            "doesn't break god-pot detection and shortens some other lines."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Toggle Tab",
        desc = "Use the tab key to toggle the tab list, not show tab list while the key is pressed. " +
            "Similar to Patcher's feature."
    )
    @ConfigEditorBoolean
    var toggleTab: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Hypixel Adverts",
        desc = "Hide text advertising the Hypixel server or store in the tablist."
    )
    @ConfigEditorBoolean
    var hideAdverts: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Fire Sale Adverts", desc = "Hide fire sales from the tablist")
    @ConfigEditorBoolean
    var hideFiresales: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Tab Background", desc = "Hides the main background in tab.")
    @ConfigEditorBoolean
    var hideTabBackground: Boolean = false

    @Expose
    @ConfigOption(name = "Advanced Player List", desc = "")
    @Accordion
    val advancedPlayerList: AdvancedPlayerListConfig = AdvancedPlayerListConfig()
}
