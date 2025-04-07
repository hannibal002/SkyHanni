package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LowHealthAlertConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a title and plays a sound when a teammate's health is low.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only while being Healer", desc = "Only show the alert if you're playing a healer.")
    @ConfigEditorBoolean
    var onlyWhileHealer: Boolean = true

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    var lowHealthAlertSound: LowHealthAlertSoundConfig = LowHealthAlertSoundConfig()
}
