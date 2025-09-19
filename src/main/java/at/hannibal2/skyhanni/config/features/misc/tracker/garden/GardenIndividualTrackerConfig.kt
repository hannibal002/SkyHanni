package at.hannibal2.skyhanni.config.features.misc.tracker.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenIndividualTrackerConfig : GenericIndividualTrackerConfig<TrackerGenericConfig>(
    { TrackerGenericConfig() },
) {
    init {
        configSet.add(this)
    }

    @ConfigOption(
        name = "Open Uptime Settings",
        desc = "The AFK timeout setting does not work for this tracker. Set in Garden Uptime Settings instead"
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val uptimeSettings: Runnable = Runnable { SkyHanniMod.feature.garden::trackerUptimeSettings.jumpToEditor() }

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
}
