package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// have to make this an abstract class and make subclasses that specify the types and add buttons
// or else moulconfig causes a crash when the user clicks a button
abstract class GenericIndividualTrackerConfig<Type : TrackerGenericConfig>(
    createType: () -> Type
) {
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
        desc = "Use the config options listed in universal tracker config instead of the ones below."
    )
    @ConfigEditorBoolean
    var useUniversalConfig = false

    fun syncSettings() {
        trackerConfig.syncSettings()
    }

    companion object TrackerSync {
        @Suppress("StorageNeedsExpose")
        val configSet: MutableSet<GenericIndividualTrackerConfig<*>> = mutableSetOf()
        val config get() = SkyHanniMod.feature.misc

        fun syncAllTrackers() {
            for (config in configSet) {
                config.syncSettings()
            }
        }
    }
}
