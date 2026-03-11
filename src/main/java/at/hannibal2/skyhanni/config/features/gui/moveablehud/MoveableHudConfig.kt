package at.hannibal2.skyhanni.config.features.gui.moveablehud

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

abstract class MoveableHudConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Allows for moving and scaling the [HUD] in the SkyHanni GUI Editor.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = MoveableHudConfig::class, field = "enabled")
    val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Shows the [HUD] outside of SkyBlock.")
    @ConfigEditorBoolean
    var showOutsideSkyblock: Boolean = false
}
