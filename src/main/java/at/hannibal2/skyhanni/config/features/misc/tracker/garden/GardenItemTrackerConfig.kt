package at.hannibal2.skyhanni.config.features.misc.tracker.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenItemTrackerConfig : ItemTrackerGenericConfig() {
    // garden trackers don't use this
    override var afkTimeout: Int = 0

    @ConfigOption(
        name = "Open Uptime Settings",
        desc = "Click to open garden uptime settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val universalTracker: Runnable = Runnable { SkyHanniMod.feature.garden::trackerUptimeSettings.jumpToEditor() }
}
