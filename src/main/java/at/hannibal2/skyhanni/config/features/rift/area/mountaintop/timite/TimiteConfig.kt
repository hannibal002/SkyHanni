package at.hannibal2.skyhanni.config.features.rift.area.mountaintop.timite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TimiteConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Helps you with mining Timite and Obsolite.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker", desc = "")
    @Accordion
    val tracker: TimiteTrackerConfig = TimiteTrackerConfig()

    @Expose
    @ConfigOption(name = "Timite Evolution Timer", desc = "Count down the time until Timite evolves with the time gun.")
    @ConfigEditorBoolean
    var evolutionTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Expiry Timer", desc = "Count down the time until Timite/Obsolite expires.")
    @ConfigEditorBoolean
    var expiryTimer: Boolean = true

    @Expose
    @ConfigLink(owner = TimiteConfig::class, field = "evolutionTimer")
    val timerPosition: Position = Position(421, -220)
}
