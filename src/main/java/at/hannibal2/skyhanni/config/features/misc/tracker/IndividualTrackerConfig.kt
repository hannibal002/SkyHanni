package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TrackerSyncEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
