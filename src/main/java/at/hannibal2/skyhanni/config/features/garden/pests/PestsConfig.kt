package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestsConfig {
    @Expose
    @ConfigOption(name = "Pest Spawn", desc = "")
    @Accordion
    val pestSpawn: PestSpawnConfig = PestSpawnConfig()

    @Expose
    @ConfigOption(name = "Pest Finder", desc = "")
    @Accordion
    val pestFinder: PestFinderConfig = PestFinderConfig()

    // TODO rename to waypoint
    @Expose
    @ConfigOption(name = "Pest Waypoint", desc = "")
    @Accordion
    val pestWaypoint: PestWaypointConfig = PestWaypointConfig()

    @Expose
    @ConfigOption(name = "Pest Timer", desc = "")
    @Accordion
    val pestTimer: PestTimerConfig = PestTimerConfig()

    @Expose
    @ConfigOption(name = "Pest Profit Tracker", desc = "")
    @Accordion
    val pestProfitTacker: PestProfitTrackerConfig = PestProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Spray", desc = "")
    @Accordion
    val spray: SprayConfig = SprayConfig()

    @ConfigOption(name = "Stereo Harmony", desc = "")
    @Accordion
    @Expose
    val stereoHarmony: StereoHarmonyConfig = StereoHarmonyConfig()

    @ConfigOption(name = "Pesthunter Profit Display", desc = "")
    @Accordion
    @Expose
    val pesthunterShop: PesthunterShopConfig = PesthunterShopConfig()

    @Expose
    @ConfigOption(name = "Bonus Chance Display", desc = "Displays your bonus pest chance and if it is enabled or not.")
    @ConfigEditorBoolean
    @FeatureToggle
    var pestChanceDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = PestsConfig::class, field = "pestChanceDisplay")
    val pestChanceDisplayPosition: Position = Position(5, -115)
}

