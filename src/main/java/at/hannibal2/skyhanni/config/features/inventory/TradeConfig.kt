package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TradeConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Displays an overlay showing the total combined value of the trade",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = TradeConfig::class, field = "enabled")
    val otherPosition: Position = Position(-300, 140)

    @Expose
    @ConfigLink(owner = TradeConfig::class, field = "enabled")
    val yourPosition: Position = Position(212, 140)
}
