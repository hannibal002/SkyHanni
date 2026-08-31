package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.common.text.StructuredText

// Wrapper around the settings of a single tracker, so they can be shown in their own config screen.
// The wrapped instance is owned by the main config and is only borrowed here for display.
// IndividualTrackerConfigGuiManager processes this class manually rather than through annotations.
class IndividualTrackerSettingsConfig(private val title: String, val settings: TrackerGenericConfig) : Config() {
    override fun getTitle(): StructuredText = title.asStructuredText()

    override fun saveNow() = SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "close-gui")
}
