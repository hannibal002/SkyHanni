package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualItemTrackerConfig
import at.hannibal2.skyhanni.features.garden.tracker.GardenTrackerTypes
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show Garden Profit.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show when:", desc = "When to show this display.")
    @ConfigEditorDraggableList
    var showWhen: MutableList<ShowWhen> = mutableListOf(ShowWhen.ALWAYS)

    enum class ShowWhen(val displayName: String) {
        FARMING("Farming"),
        KILLING_PESTS("Killing pests"),
        ON_BARN("On Barn Plot"),
        ALWAYS("Always when on garden")
        ;
        override fun toString(): String = displayName
    }

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
            GardenProfitTextEntry.PROFIT_LIST,

            GardenProfitTextEntry.CROP_DROPS,
            GardenProfitTextEntry.ITEM_PROFIT,
            GardenProfitTextEntry.COINS_SPENT,
            GardenProfitTextEntry.SPACER_2,
            GardenProfitTextEntry.TOTAL_PROFIT,
            GardenProfitTextEntry.PROFIT_PER_HOUR,
            GardenProfitTextEntry.BPS
        )
    )

    enum class GardenProfitTextEntry(private val displayName: String) {
        TITLE("§6§lGarden Profit Tracker"),
        CROP_DROPS("§eHarvested Crop Profit: §650.2m"),
        PROFIT_LIST("Item Profit List"),
        ITEM_PROFIT("§eItem Drop Profit: §672.8m"),
        COINS_SPENT("§eCrops Spent: §c-20.2m"),
        TOTAL_PROFIT("§eTotal Profit: §6802.2m"),
        PROFIT_PER_HOUR("§eProfit Per Hour: §622.2m"),
        BPS("§eBlocks Per Second: §b17.2"),
        SPACER(""),
        SPACER_2("")
        ;
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(
        name = "Base Crops use NPC Price",
        desc = "Use npc sell price for base crops (eg. Melon Slice, Carrot) as they are prone to manipulation."
    )
    @ConfigEditorBoolean
    val useNpcPrice: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Harvested Crops in Drops", desc = "Show crops you gain by breaking blocks in the item drops list.")
    @ConfigEditorBoolean
    val includeHarvestedCrops: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Harvest Crops Compact Mode",
        desc = "Choose whether to show crops should be shown/priced as their base form or one of their compacted forms."
    )
    @ConfigEditorDropdown
    val compactMode: Property<HarvestedCropsMode> = Property.of(HarvestedCropsMode.BASE)

    enum class HarvestedCropsMode(val displayName: String) {
        BASE("Base"),
        COMPACTED("Compacted"),
        SUPER_COMPACTED("Super-Compacted")
        ;
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(name = "Coins Per Copper", desc = "Set the amount of coins each copper is worth.")
    @ConfigEditorSlider(minValue = 1000f, maxValue = 10000f, minStep = 250f)
    val coinsPerCopper: Property<Int> = Property.of(5000)

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
