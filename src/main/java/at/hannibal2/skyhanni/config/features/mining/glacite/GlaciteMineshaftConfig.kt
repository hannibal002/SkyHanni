package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GlaciteMineshaftConfig {
    @Expose
    @ConfigOption(name = "Mineshaft Waypoints", desc = "General waypoints inside the Mineshaft.")
    @Accordion
    val mineshaftWaypoints: MineshaftWaypointsConfig = MineshaftWaypointsConfig()

    @Expose
    @ConfigOption(name = "Corpse Locator", desc = "")
    @Accordion
    val corpseLocator: CorpseLocatorConfig = CorpseLocatorConfig()

    @Expose
    @ConfigOption(name = "Corpse Tracker", desc = "")
    @Accordion
    val corpseTracker: CorpseTrackerConfig = CorpseTrackerConfig()

    @Expose
    @ConfigOption(
        name = "Share Waypoint Location",
        desc = "Share the location of the nearest waypoint upon key press.\n" +
            "§eYou can share the location even if it has already been shared!"
    )
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var shareWaypointLocation: Int = KeyboardManager.KEY_UNKNOWN

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
