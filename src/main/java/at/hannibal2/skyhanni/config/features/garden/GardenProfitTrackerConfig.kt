package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.GardenProfitTrackerConfig.GardenProfitTextEntry
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.GenericIndividualTrackerConfig.TrackerSync.config
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualItemTrackerConfig
import at.hannibal2.skyhanni.features.garden.tracker.GardenTrackerTypes
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all items you pick up when killing pests.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Profit Types", desc = "What sources of profit/spending should be included.")
    @ConfigEditorDraggableList
    val profitTypes: Property<MutableList<GardenTrackerTypes>> =
        Property.of(
            mutableListOf(
                GardenTrackerTypes.VISITORS,
                GardenTrackerTypes.PESTS,
                GardenTrackerTypes.COMPOSTER,
                GardenTrackerTypes.BREAKING_CROPS
            )
        )

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay." +
            "\nOnly applicable lines will be shown for each bucket."
    )
    @ConfigEditorDraggableList
    val textFormat: Property<MutableList<GardenProfitTextEntry>> = Property.of(
        mutableListOf(
            GardenProfitTextEntry.TITLE,

            GardenProfitTextEntry.PROFIT_LIST
        )
    )

    enum class GardenProfitTextEntry(private val displayName: String) {
        TITLE("§e§lGarden Profit Tracker"),
        PROFIT_LIST("Item List"),
        COPPER("§cCopper: 62,072 §6298.8m"),
        BITS("§bBits: 4.2k §642m"),
        VISITOR_SPENT("§7Visitor Coins Spent: §c-782k"),
        COMPOSTER_SPENT("§7Composter Coins Spent: §c-782k")
    }

    @ConfigOption(name = "Visitor Price Options", desc = "Set coins per copper and bit in Visitor Profit Tracker Settings")
    @ConfigEditorButton(buttonText = "OPEN")
    val visitorButton: Runnable = Runnable { SkyHanniMod.feature.garden.visitors.dropsStatistics::coinsPerCopper }

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedGardenIndividualItemTrackerConfig = TimedGardenIndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = GardenProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
