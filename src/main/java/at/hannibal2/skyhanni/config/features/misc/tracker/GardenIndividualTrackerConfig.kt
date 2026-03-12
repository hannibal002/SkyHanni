package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualTrackerConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenIndividualTrackerConfig : IndividualTrackerConfig() {
    @ConfigOption(
        name = "Open Uptime Settings",
        desc = "The AFK timeout setting does not work for this tracker. Set in Garden Uptime Settings instead"
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val uptimeSettings: Runnable = Runnable { SkyHanniMod.feature.garden::trackerUptimeSettings.jumpToEditor() }
}
