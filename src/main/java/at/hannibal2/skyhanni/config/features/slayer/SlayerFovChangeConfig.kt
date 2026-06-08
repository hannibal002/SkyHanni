package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerFovChangeConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Change the FOV while the boss is spawned\"")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Target FOV", desc = "")
    @ConfigEditorSlider(minValue = 30f, maxValue = 110f, minStep = 1f)
    var targetFov: Float = 75f
}
