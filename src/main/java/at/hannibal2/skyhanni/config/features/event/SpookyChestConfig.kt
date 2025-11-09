package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpookyChestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a title when a Trick or Treat/Party Chest appears.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Play Sound", desc = "Play a sound when triggering a spooky/party chest.")
    @ConfigEditorBoolean
    var playSound: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Title", desc = "Only show the name of the chest in the title.")
    @ConfigEditorBoolean
    var compactTitle: Boolean = false

}
