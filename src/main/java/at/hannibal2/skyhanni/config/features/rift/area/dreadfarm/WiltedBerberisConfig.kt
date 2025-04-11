package at.hannibal2.skyhanni.config.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WiltedBerberisConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show Wilted Berberis helper.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only on Farmland", desc = "Only show the helper while standing on Farmland blocks.")
    @ConfigEditorBoolean
    var onlyOnFarmland: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide the Wilted Berberis particles.")
    @ConfigEditorBoolean
    var hideParticles: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mute Others Sounds",
        desc = "Mute nearby Wilted Berberis sounds while not holding a Wand of Farming or not standing on Farmland blocks."
    )
    @ConfigEditorBoolean
    var muteOthersSounds: Boolean = true
}
