package at.hannibal2.skyhanni.config.features.mining.glacite

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GlaciteMineshaftConfig {
    @Expose
    @ConfigOption(name = "Mineshaft Waypoints", desc = "General waypoints inside the Mineshaft.")
    @Accordion
    val waypointsConfig: MineshaftWaypointsConfig = MineshaftWaypointsConfig()

    @Expose
    @ConfigOption(name = "Corpse Tracker", desc = "")
    @Accordion
    val corpseTracker: CorpseTrackerConfig = CorpseTrackerConfig()

    @Expose
    @ConfigOption(name = "Mineshaft Detection", desc = "")
    @Accordion
    val mineshaftDetectionConfig: MineshaftDetectionConfig = MineshaftDetectionConfig()

    @Expose
    @ConfigOption(name = "Mineshaft Timer", desc = "")
    @Accordion
    val mineshaftTimerConfig: MineshaftTimerConfig = MineshaftTimerConfig()

    @Expose
    @ConfigOption(name = "Organ Donor Accessory", desc = "")
    @Accordion
    val organDonorAccessoryConfig: OrganDonorAccessoryConfig = OrganDonorAccessoryConfig()
}
