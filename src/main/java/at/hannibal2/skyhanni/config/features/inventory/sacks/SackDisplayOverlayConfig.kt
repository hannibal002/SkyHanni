package at.hannibal2.skyhanni.config.features.inventory.sacks

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SackDisplayOverlayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track the amounts of specified items in your sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = true

    @Expose
    @ConfigLink(owner = SackDisplayOverlayConfig::class, field = "enabled")
    val position: Position = Position(150, 150)

}