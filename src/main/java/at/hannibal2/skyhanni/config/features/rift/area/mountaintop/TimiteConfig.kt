package at.hannibal2.skyhanni.config.features.rift.area.mountaintop

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TimiteConfig {
    @Expose
    @ConfigOption(name = "Timite Evolution Timer", desc = "Count down the time until Timite evolves with the time gun.")
    @ConfigEditorBoolean
    var evolutionTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Expiry Timer", desc = "Count down the time until Timite/Obsolite expires.")
    @ConfigEditorBoolean
    var expiryTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Timite Tracker", desc = "Tracks collected Timite ores and shows mote profit.")
    @ConfigEditorBoolean
    var tracker: Boolean = false

    @Expose
    @ConfigOption(name = "Only Show While Holding", desc = "Only shows the tracker while holding the Timite pickaxes or the Time Gun.")
    @ConfigEditorBoolean
    var onlyShowWhileHolding: Boolean = false

    @Expose
    @ConfigLink(owner = TimiteConfig::class, field = "evolutionTimer")
    val timerPosition: Position = Position(421, -220)

    @Expose
    @ConfigLink(owner = TimiteConfig::class, field = "tracker")
    val trackerPosition: Position = Position(-201, -220)
}
