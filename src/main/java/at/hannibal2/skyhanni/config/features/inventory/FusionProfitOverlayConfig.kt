package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FusionProfitOverlayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables the Fusion Profit Overlay.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = FusionProfitOverlayConfig::class, field = "enabled")
    val displayPosition: Position = Position(174, 139)
}
