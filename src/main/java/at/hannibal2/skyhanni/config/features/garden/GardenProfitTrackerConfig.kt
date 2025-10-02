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
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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
        TITLE("§6§lGarden Profit Tracker"),
        CROP_DROPS("§7Crop Drops: §650.2m"),
        PROFIT_LIST("§eItem Drops:\n[Item List]"),
        CROPS_SPENT("§7Crops Spent: §c-20.2m"),
        BPS("§7Blocks Per Second: §")
    }

    @Expose
    @ConfigOption(name = "Copper In Profit Calculations", desc = "Include Copper Profit in Total Profit. Set the coins per copper below.")
    @ConfigEditorBoolean
    val includeCopper: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Coins Per Copper", desc = "Set the amount of coins each copper is worth.")
    @ConfigEditorSlider(minValue = 1000f, maxValue = 10000f, minStep = 250f)
    val coinsPerCopper: Property<Int> = Property.of(5000)

    @Expose
    @ConfigOption(name = "Bits In Profit Calculations", desc = "Include Bits Profit in Total Profit. Set the coins per bit below.")
    @ConfigEditorBoolean
    val includeBits: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Bits Per Copper", desc = "Set the amount of bits each copper is worth.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 2000f, minStep = 100f)
    val coinsPerBit: Property<Int> = Property.of(1000)

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
