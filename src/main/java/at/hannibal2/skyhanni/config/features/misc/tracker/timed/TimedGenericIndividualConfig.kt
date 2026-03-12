package at.hannibal2.skyhanni.config.features.misc.tracker.timed

import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class TimedGenericIndividualConfig<out Type : TrackerGenericConfig>(
    createType: () -> Type
) : GenericIndividualTrackerConfig<Type>(createType) {
    @Expose
    @ConfigOption(name = "Timed Tracker", desc = "Timed Tracker Settings")
    @Accordion
    val timedTracker: TimedTrackerConfig = TimedTrackerConfig()

    @ConfigOption(
        name = "Universal Settings",
        desc = "Click to open the universal tracker settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync Settings",
        desc = "Sync these settings with universal tracker settings. \n" +
            "§c§lTHIS WILL OVERRIDE ALL OF YOUR CURRENT TRACKER SETTINGS!"
    )
    @ConfigEditorButton(buttonText = "SYNC")
    val syncButton: Runnable = Runnable { syncSettings() }

    override fun syncSettings() {
        super.syncSettings()
        timedTracker.syncSettings()
    }
}
