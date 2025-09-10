package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.TrackerSyncEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class UniversalTrackerConfig: ItemTrackerGenericConfig() {
    @ConfigOption(
        name = "Sync All Trackers",
        desc = "Sync all Skyhanni Trackers with these settings."
    )
    @ConfigEditorButton(buttonText = "Sync")
    val sync: Runnable = Runnable { TrackerSyncEvent.post() }

    // Doing this here since SkyHanniTracker isn't a SkyHanniModule
    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(95, "misc.tracker.hideItemTrackersOutsideInventory", "misc.tracker.hideOutsideInventory")
        }
    }
}
