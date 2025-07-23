package at.hannibal2.skyhanni.config.features.slayer.spider

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiderConfig {

    @Expose
    @ConfigOption(name = "Mark When Invincible", desc = "Highlight the Tarantula Slayer tier 5 when the hatchlings are alive.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInvincible: Boolean = true
}
