package at.hannibal2.skyhanni.config.features.misc.tracker.timed

import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class TimedGenericIndividualConfig<out Type : TrackerGenericConfig>(
    createType: () -> Type
): GenericIndividualTrackerConfig<Type>(createType) {
    @Expose
    @ConfigOption(name = "Timed Tracker", desc = "Timed Tracker Settings")
    @Accordion
    val timedTracker: TimedTrackerConfig = TimedTrackerConfig()

    override fun syncSettings() {
        super.syncSettings()
        timedTracker.syncSettings()
    }
}
