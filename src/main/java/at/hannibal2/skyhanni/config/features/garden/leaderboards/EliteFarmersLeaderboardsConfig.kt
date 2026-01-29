package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.PositionList
import at.hannibal2.skyhanni.features.garden.leaderboarddisplays.EliteLeaderboards
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class EliteFarmersLeaderboardsConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable leaderboard features.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Leaderboards",
        desc = "Choose what leaderboards to enable.\n" +
            "Per-leaderboard settings below.\n" +
            "Leaderboards provided by §eelitebot.dev"
    )
    @ConfigEditorDraggableList
    val display: Property<MutableList<EliteLeaderboards>> = Property.of(
        mutableListOf(EliteLeaderboards.CROP, EliteLeaderboards.PEST, EliteLeaderboards.WEIGHT)
    )

    @Expose
    @ConfigLink(owner = EliteFarmersLeaderboardsConfig::class, field = "display")
    val displayPositions: PositionList = PositionList(EliteLeaderboards.entries.size)

    @Expose
    @ConfigOption(name = "Farming Weight Display", desc = "")
    @Accordion
    val farmingWeightLeaderboard: WeightLeaderboardConfig = WeightLeaderboardConfig()

    @Expose
    @ConfigOption(name = "Crop Collection Display", desc = "")
    @Accordion
    val cropCollectionLeaderboard: CropCollectionLeaderboardConfig = CropCollectionLeaderboardConfig()

    @Expose
    @ConfigOption(name = "Pest Kills Display", desc = "")
    @Accordion
    val pestKillsLeaderboard: PestKillsLeaderboardConfig = PestKillsLeaderboardConfig()
}
