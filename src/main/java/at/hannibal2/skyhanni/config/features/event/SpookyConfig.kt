package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpookyConfig {
    @Expose
    @ConfigOption(name = "Trick or Treat Chest Alert", desc = "Shows a title when a Trick or Treat/Party Chest appears.")
    @ConfigEditorBoolean
    @FeatureToggle
    var spookyChestAlert: Boolean = false
}
