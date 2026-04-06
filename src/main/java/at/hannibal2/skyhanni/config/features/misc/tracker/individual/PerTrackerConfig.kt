package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.ReflectionUtils
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Per-tracker wrapper that holds an instance of [Settings] and exposes sync controls.
 *
 * **Instantiation — two paths:**
 * - `PerTrackerConfig<SomeSettings>()` from a non-subclass context (e.g. a config property
 *   initializer): the `protected` primary constructor is unreachable, so Kotlin resolves the
 *   call to the companion [invoke] operator, which captures the reified type directly —
 *   no reflection required.
 * - `class Foo : PerTrackerConfig<SomeSettings>()` (proper subclass): Kotlin calls the
 *   protected constructor with `settingsClass = null`, falling back to [ReflectionUtils.findGenericSuperclassTypeArgument]
 *   which walks the hierarchy with type-variable substitution.
 *
 * Registering in [TrackerSync.configSet] on init allows bulk sync and universal-config
 * propagation via [TrackerSync.syncAllTrackers] and [TrackerSync.setUseUniversalConfig].
 *
 * @param Settings the [TrackerSettings] subclass whose config options appear inside
 *   the "Individual Tracker Settings" accordion.
 */
open class PerTrackerConfig<out Settings : TrackerSettings> protected constructor(
    settingsClass: Class<@UnsafeVariance Settings>? = null,
) {
    init {
        configSet.add(this)
    }

    @Suppress("UNCHECKED_CAST")
    private val settingsClass = settingsClass ?: findGenericSuperclassTypeArgument<PerTrackerConfig<*>, Settings>()

    @Expose
    @ConfigOption(name = "Individual Tracker Settings", desc = "")
    @Accordion
    val trackerConfig: Settings = this.settingsClass.getConstructor().newInstance()

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

        /** Creates a [PerTrackerConfig] using the reified [S] directly, bypassing reflection. */
        inline operator fun <reified S : TrackerSettings> invoke(): PerTrackerConfig<S> =
            object : PerTrackerConfig<S>(S::class.java) {}

        fun syncAllTrackers() {
            configSet.onEach { it.syncSettings() }
            ChatUtils.debug("Synced All Trackers")
        }

        fun setUseUniversalConfig(useUniversalConfig: Boolean) = configSet.onEach {
            it.useUniversalConfig = useUniversalConfig
        }
    }
}
