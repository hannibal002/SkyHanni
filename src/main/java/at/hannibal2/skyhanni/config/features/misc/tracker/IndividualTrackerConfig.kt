package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class IndividualTrackerConfig : GenericIndividualTrackerConfig<TrackerGenericConfig>(
    { TrackerGenericConfig() }
) {
    init {
        configSet.add(this)
    }
    @ConfigOption(
        name = "Universal Settings",
        desc = "Click to open the universal tracker settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync",
        desc = "Sync these settings with universal tracker settings"
    )
    @ConfigEditorButton(buttonText = "SYNC")
    val syncButton: Runnable = Runnable { syncSettings() }
}
