package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class IndividualItemTrackerConfig : IndividualTrackerConfig() {
    @ConfigOption(
        name = "Universal Settings",
        desc = "Click to open the universal tracker settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    override val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync Settings",
        desc = "Sync these settings with universal tracker settings.\n§c§lTHIS WILL OVERRIDE ALL OF YOUR CURRENT TRACKER SETTINGS!"
    )
    @ConfigEditorButton(buttonText = "SYNC")
    override val syncButton: Runnable = Runnable { syncSettings() }
}
