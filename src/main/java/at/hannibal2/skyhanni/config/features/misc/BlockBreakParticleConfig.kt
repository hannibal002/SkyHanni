package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BlockBreakParticleConfig {

    @JvmField
    @Expose
    @ConfigOption(name = "Block Break Particles", desc = "Hide Block Break particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideBlockBreakParticles: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Only on garden", desc = "Hide Block Break particles only on garden")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideBlockBreakParticlesGarden: Boolean = false
}
