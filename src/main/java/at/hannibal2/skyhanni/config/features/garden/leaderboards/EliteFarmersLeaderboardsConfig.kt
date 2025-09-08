package at.hannibal2.skyhanni.config.features.garden.leaderboards

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EliteFarmersLeaderboardsConfig {
    @Expose
    @ConfigOption(name = "Farming Weight Display", desc = "")
    @Accordion
    val farmingWeightDisplay: WeightLeaderboardConfig = WeightLeaderboardConfig()

    @Expose
    @ConfigOption(name = "Crop Collection Display", desc = "")
    @Accordion
    val cropCollectionDisplay: CropCollectionLeaderboardConfig = CropCollectionLeaderboardConfig()

    @Expose
    @ConfigOption(name = "Pest Kills Display", desc = "")
    @Accordion
    val pestKillsDisplay: PestKillsLeaderboardConfig = PestKillsLeaderboardConfig()
}
