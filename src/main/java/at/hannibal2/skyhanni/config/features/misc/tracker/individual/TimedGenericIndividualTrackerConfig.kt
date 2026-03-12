package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerGenericConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class TimedGenericIndividualTrackerConfig<out Type : TrackerGenericConfig> : GenericIndividualTrackerConfig<Type>() {
    @Expose
    @ConfigOption(name = "Timed Tracker", desc = "Timed Tracker Settings")
    @Accordion
    val timedTracker: TimedTrackerConfig = TimedTrackerConfig()

    override fun syncSettings() {
        timedTracker.syncSettings()
        super.syncSettings()
    }
}
