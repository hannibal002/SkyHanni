package at.hannibal2.skyhanni.config.features.event.gifting

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GiftTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the gift profit tracker.")
    @ConfigEditorBoolean
    override var enabled: Boolean = false

    @ConfigOption(
        name = "§cNote",
        desc = "§cDue to the complexities of gifts leaving and re-entering the inventory or stash, gift usage is not auto-tracked. " +
            "§cUse §e/shaddusedgifts §cto manually add gifts used."
    )
    @ConfigEditorInfoText
    var note: String = ""

    @Expose
    @ConfigOption(name = "Holding Gift", desc = "Only show the tracker while holding a gift.")
    @ConfigEditorBoolean
    var holdingGift: Boolean = false

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = GiftTrackerConfig::class, field = "enabled")
    override val position: Position = Position(-274, 0)
}
