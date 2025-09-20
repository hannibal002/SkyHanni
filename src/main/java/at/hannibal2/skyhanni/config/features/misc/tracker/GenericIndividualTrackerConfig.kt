package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// have to make this an abstract class and make subclasses that specify the types and add buttons
// or else moulconfig causes a crash when the user clicks a button
class GenericIndividualTrackerConfig<out Type : TrackerGenericConfig>(
    createType: () -> Type
) {
    init {
        configSet.add(this)
    }
    @Expose
    @ConfigOption(
        name = "Individual Tracker Settings",
        desc = ""
    )
    @Accordion
    val trackerConfig: Type = createType()

    // the first time a user launches the game with a build that includes individual tracker configs,
    // we sync every individual tracker with the universal tracker,
    // so better to leave this off to avoid confusion when players don't read
    @Expose
    @ConfigOption(
        name = "Use Universal Settings",
        desc = "Use the config options listed in the universal tracker config instead of the ones above."
    )
    @ConfigEditorBoolean
    var useUniversalConfig = false

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

    open fun syncSettings() {
        trackerConfig.syncSettings()
        ChatUtils.debug("Synced tracker!")
    }

    companion object TrackerSync {
        @Suppress("StorageNeedsExpose")
        val configSet: MutableSet<GenericIndividualTrackerConfig<*>> = mutableSetOf()
        val config get() = SkyHanniMod.feature.misc

        fun syncAllTrackers() {
            for (config in configSet) {
                config.syncSettings()
            }
            ChatUtils.debug("Synced All Trackers")
        }

        fun setUseUniversalConfig(useUniversalConfig: Boolean) {
            for (config in configSet) {
                config.useUniversalConfig = useUniversalConfig
            }
        }
    }
}
