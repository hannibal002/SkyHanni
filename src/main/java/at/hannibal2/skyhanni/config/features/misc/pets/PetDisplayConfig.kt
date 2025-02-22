package at.hannibal2.skyhanni.config.features.misc.pets

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = PetDisplayConfig::class, field = "enabled")
    var position: Position = Position(-330, -15, false, true)

    @Expose
    @ConfigOption(name = "Level Ring", desc = "Show a ring to indicate level progression.")
    @ConfigEditorBoolean
    var levelRing: Boolean = true
}
