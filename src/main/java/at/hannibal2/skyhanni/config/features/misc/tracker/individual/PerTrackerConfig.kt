package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Per-tracker wrapper that holds an instance of [Settings] and exposes sync controls.
 *
 * Each concrete tracker config declares its own [trackerConfig] property typed to the
 * appropriate [Settings] subclass (e.g. `PerTrackerConfig<GardenItemTrackerSettings>`).
 * The settings instance is instantiated reflectively because the type parameter is erased
 * at runtime - see [settingsCtor].
 *
 * Registering in [TrackerSync.configSet] on init allows bulk sync and universal-config
 * propagation via [TrackerSync.syncAllTrackers] and [TrackerSync.setUseUniversalConfig].
 *
 * @param Settings the [TrackerSettings] subclass whose config options appear inside
 *   the "Individual Tracker Settings" accordion.
 */
open class PerTrackerConfig<out Settings : TrackerSettings> {
    init {
        configSet.add(this)
    }

    // Instantiate Settings reflectively because the type argument is erased at runtime.
    // This mirrors the pattern used in TimedTrackerData for session creation.
    private val settingsCtor by lazy {
        findGenericSuperclassTypeArgument<PerTrackerConfig<*>, Settings>().getConstructor()
    }

    @Expose
    @ConfigOption(name = "Individual Tracker Settings", desc = "")
    @Accordion
    val trackerConfig: Settings = settingsCtor.newInstance()

    @ConfigOption(
        name = "Universal Settings",
        desc = "Click to open the universal tracker settings.",
    )
    @ConfigEditorButton(buttonText = "OPEN")
    open val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync Settings",
        desc = "Sync these settings with universal tracker settings.\n" +
            "§c§lTHIS WILL OVERRIDE ALL OF YOUR CURRENT TRACKER SETTINGS!",
    )
    @ConfigEditorButton(buttonText = "SYNC")
    open val syncButton: Runnable = Runnable { syncSettings() }

    // the first time a user launches the game with a build that includes individual tracker configs,
    // we sync every individual tracker with the universal tracker,
    // so better to leave this off to avoid confusion when players don't read
    @Expose
    @ConfigOption(
        name = "Use Universal Settings",
        desc = "Use the config options listed in the universal tracker config instead of the ones above.",
    )
    @ConfigEditorBoolean
    var useUniversalConfig = false

    open fun syncSettings() {
        trackerConfig.syncSettings()
        ChatUtils.debug("Synced tracker!")
    }

    companion object TrackerSync {
        @Suppress("StorageNeedsExpose")
        val configSet: MutableSet<PerTrackerConfig<*>> = mutableSetOf()
        val config get() = SkyHanniMod.feature.misc

        fun syncAllTrackers() {
            configSet.onEach { it.syncSettings() }
            ChatUtils.debug("Synced All Trackers")
        }

        fun setUseUniversalConfig(useUniversalConfig: Boolean) = configSet.onEach {
            it.useUniversalConfig = useUniversalConfig
        }
    }
}
