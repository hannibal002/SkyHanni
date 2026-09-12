package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.garden.GardenIndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FarmingProfitTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track crops, special drops, and Bountiful coins while farming.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only Show With Tool", desc = "Only show while holding a farming tool or shortly after farming.")
    @ConfigEditorBoolean
    var onlyWithFarmingTool: Boolean = true

    @Expose
    @ConfigOption(name = "Show After Farming", desc = "Seconds to keep showing the tracker after farming.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    var showAfterFarming: Int = 10

    @Expose
    @ConfigOption(
        name = "Tracked Sources",
        desc = "Remove entries to stop adding those farming sources to the tracker.",
    )
    @ConfigEditorDraggableList
    val trackedSources: MutableList<TrackedSource> = mutableListOf(
        TrackedSource.CROPS,
        TrackedSource.MOOSHROOM_COW,
        TrackedSource.BOUNTIFUL,
        TrackedSource.BLESSED,
        TrackedSource.RARE_CROPS,
        TrackedSource.CROP_FEVER,
        TrackedSource.PESTS,
        TrackedSource.VISITORS,
    )

    @Expose
    @ConfigOption(
        name = "Displayed Stats",
        desc = "Remove entries to hide those farming stat lines from the tracker.",
    )
    @ConfigEditorDraggableList
    val displayedStats: MutableList<DisplayStat> = mutableListOf(
        DisplayStat.CROPS_TRACKED,
        DisplayStat.BLOCKS_BROKEN,
        DisplayStat.RARE_CROP_DROPS,
        DisplayStat.BLESSED_DROPS,
        DisplayStat.CROP_FEVERS,
        DisplayStat.CROP_FEVER_DROPS,
        DisplayStat.PESTS_KILLED,
        DisplayStat.VISITORS_SERVED,
        DisplayStat.BOUNTIFUL_COINS,
    )

    enum class TrackedSource(val displayName: String) {
        CROPS("Crops"),
        MOOSHROOM_COW("Mooshroom Cow"),
        BOUNTIFUL("Bountiful"),
        BLESSED("Blessed"),
        RARE_CROPS("Rare Crops"),
        CROP_FEVER("Crop Fever"),
        PESTS("Pests"),
        VISITORS("Visitors"),
        ;

        override fun toString() = displayName
    }

    enum class DisplayStat(private val displayName: String) {
        CROPS_TRACKED("Crops Tracked"),
        BLOCKS_BROKEN("Blocks Broken"),
        RARE_CROP_DROPS("Rare Crop Drops"),
        BLESSED_DROPS("Blessed Drops"),
        CROP_FEVERS("Crop Fevers"),
        CROP_FEVER_DROPS("Crop Fever Drops"),
        PESTS_KILLED("Pests Killed"),
        VISITORS_SERVED("Visitors Served"),
        BOUNTIFUL_COINS("Bountiful Coins"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    val perTrackerConfig: GardenIndividualItemTrackerConfig = GardenIndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = FarmingProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(80, 60)
}
