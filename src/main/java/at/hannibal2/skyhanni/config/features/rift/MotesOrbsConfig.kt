package at.hannibal2.skyhanni.config.features.rift

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MotesOrbsConfig {
    @Expose
    @ConfigOption(name = "Highlight Motes Orbs", desc = "Highlight flying Motes Orbs.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Size", desc = "Set render size for highlighted Motes Orbs.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 5f)
    var size: Int = 3

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide normal Motes Orbs particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideParticles: Boolean = false
}
