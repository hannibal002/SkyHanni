package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig.TrackerSync.setUseUniversalConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig.TrackerSync.syncAllTrackers
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class UniversalTrackerConfig : ItemTrackerGenericConfig() {
    @ConfigOption(
        name = "Use Universal Settings",
        desc = "Sets the \"Use Universal Settings\" option in all tracker configs to True."
    )
    @ConfigEditorButton(buttonText = "SET")
    val useUniversal: Runnable = Runnable { setUseUniversalConfig(true) }

    @ConfigOption(
        name = "Sync All Trackers",
        desc = "Sync all Skyhanni Trackers with these settings.\n§c§lTHIS WILL OVERRIDE ALL OF YOUR INDIVIDUAL TRACKER SETTINGS!"
    )
    @ConfigEditorButton(buttonText = "Sync")
    val sync: Runnable = Runnable { syncAllTrackers() }

}
