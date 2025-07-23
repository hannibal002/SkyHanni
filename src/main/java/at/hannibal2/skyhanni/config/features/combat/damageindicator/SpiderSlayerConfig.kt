package at.hannibal2.skyhanni.config.features.combat.damageindicator

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SpiderSlayerConfig {

    @Expose
    @ConfigOption(name = "Text When Invincible", desc = "Show a text next to the Tarantula Slayer tier 5 when the hatchlings are alive.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showInvincible: Boolean = true
}
