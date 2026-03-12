package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerGenericConfig
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class IndividualTrackerConfig : GenericIndividualTrackerConfig<TrackerGenericConfig>()
open class IndividualItemTrackerConfig : GenericIndividualTrackerConfig<ItemTrackerGenericConfig>()
abstract class GenericIndividualTrackerConfig<out Type : TrackerGenericConfig> {
    init {
        configSet.add(this)
    }

    private val outTypeCtor by lazy {
        findGenericSuperclassTypeArgument<GenericIndividualTrackerConfig<*>, Type>().getConstructor()
    }

    @Expose
    @ConfigOption(name = "Individual Tracker Settings", desc = "")
    @Accordion
    val trackerConfig: Type = outTypeCtor.newInstance()

    @ConfigOption(
        name = "Universal Settings",
        desc = "Click to open the universal tracker settings."
    )
    @ConfigEditorButton(buttonText = "OPEN")
    open val universalTracker: Runnable = Runnable { config::tracker.jumpToEditor() }

    @ConfigOption(
        name = "Sync Settings",
        desc = "Sync these settings with universal tracker settings. \n" +
            "§c§lTHIS WILL OVERRIDE ALL OF YOUR CURRENT TRACKER SETTINGS!"
    )
    @ConfigEditorButton(buttonText = "SYNC")
    open val syncButton: Runnable = Runnable { syncSettings() }

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

    open fun syncSettings() {
        trackerConfig.syncSettings()
        ChatUtils.debug("Synced tracker!")
    }

    companion object TrackerSync {
        @Suppress("StorageNeedsExpose")
        val configSet: MutableSet<GenericIndividualTrackerConfig<*>> = mutableSetOf()
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
