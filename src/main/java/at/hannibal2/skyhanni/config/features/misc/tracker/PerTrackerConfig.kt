package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.MiscConfig
import at.hannibal2.skyhanni.events.TrackerSyncEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PerTrackerConfig<Tracker: TrackerGenericConfig>(
    createTracker: () -> Tracker
) {
    init {
        configSet.add(this)
    }

    @Expose
    @ConfigOption(
        name = "Use Universal Tracker Settings",
        desc = "Use the config options listed in universal tracker config instead of the ones below."
    )
    @ConfigEditorBoolean
    val useUniversalConfig = false

    @ConfigOption(
        name = "Open Universal Tracker Settings",
        desc = "Click to open the universal tracker settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync with Universal Settings",
        desc = "Sync these settings with universal tracker settings"
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val sounds: Runnable = Runnable { syncSettings() }

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val trackerConfig: Tracker = createTracker()

    fun syncSettings() {
        trackerConfig.syncSettings()
    }

    @SkyHanniModule
    companion object TrackerSync {
        private val configSet: MutableSet<PerTrackerConfig<*>> = mutableSetOf()
        private val config = SkyHanniMod.feature.misc

        @HandleEvent
        fun onTrackerSync(event: TrackerSyncEvent) {
            for (config in configSet) {
                config.syncSettings()
            }
        }
    }
}
