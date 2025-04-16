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
    var pestSpawn: PestSpawnConfig = PestSpawnConfig()

    @Expose
    @ConfigOption(name = "Pest Finder", desc = "")
    @Accordion
    var pestFinder: PestFinderConfig = PestFinderConfig()

    @Expose
    @ConfigOption(name = "Pest Waypoint", desc = "")
    @Accordion
    var pestWaypoint: PestWaypointConfig = PestWaypointConfig()

    @Expose
    @ConfigOption(name = "Pest Timer", desc = "")
    @Accordion
    var pestTimer: PestTimerConfig = PestTimerConfig()

    @Expose
    @ConfigOption(name = "Pest Profit Tracker", desc = "")
    @Accordion
    var pestProfitTacker: PestProfitTrackerConfig = PestProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Spray", desc = "")
    @Accordion
    var spray: SprayConfig = SprayConfig()

    @ConfigOption(name = "Stereo Harmony", desc = "")
    @Accordion
    @Expose
    var stereoHarmony: StereoHarmonyConfig = StereoHarmonyConfig()

    @ConfigOption(name = "Pesthunter Profit Display", desc = "")
    @Accordion
    @Expose
    var pesthunterShop: PesthunterShopConfig = PesthunterShopConfig()

    @Expose
    @ConfigOption(name = "Bonus Chance Display", desc = "Displays your bonus pest chance and if it is enabled or not.")
    @ConfigEditorBoolean
    @FeatureToggle
    var pestChanceDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = PestsConfig::class, field = "pestChanceDisplay")
    var pestChanceDisplayPosition: Position = Position(5, -115, false, true)
}

