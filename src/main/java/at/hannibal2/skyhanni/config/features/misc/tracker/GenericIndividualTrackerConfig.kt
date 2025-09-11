package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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

    @Expose
    @ConfigOption(
        name = "Use Universal Settings",
        desc = "Use the config options listed in universal tracker config instead of the ones below."
    )
    @ConfigEditorBoolean
    var useUniversalConfig = true

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
