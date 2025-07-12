package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ParticleHiderConfig {
    @JvmField
    @Expose
    @ConfigOption(name = "Blaze Particles", desc = "Hide Blaze particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideBlazeParticles: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Enderman Particles", desc = "Hide Enderman particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideEndermanParticles: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Fireball Particles", desc = "Hide fireball particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFireballParticles: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Fire Particles", desc = "Hide particles from the fire block.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFireBlockParticles: Boolean = false

    @Expose
    @ConfigOption(name = "Smoke Particles", desc = "Hide smoke particles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideSmokeParticles: Boolean = false

    @Expose
    @ConfigOption(name = "Far Particles", desc = "Hide particles that are more than 40 blocks away.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFarParticles: Boolean = false

    @Expose
    @ConfigOption(
        name = "Close Redstone Particles",
        desc = "Hide Redstone particles around the player (appear for some potion effects)."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideCloseRedstoneParticles: Boolean = false
}
