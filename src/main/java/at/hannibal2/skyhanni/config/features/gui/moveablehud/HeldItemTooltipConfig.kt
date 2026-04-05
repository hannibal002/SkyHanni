package at.hannibal2.skyhanni.config.features.gui.moveablehud

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HeldItemTooltipConfig : MoveableHudConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Allows for moving and scaling the held item tooltip in the SkyHanni GUI Editor.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = HeldItemTooltipConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Shows the held item tooltip outside of SkyBlock.")
    @ConfigEditorBoolean
    override var showOutsideSkyblock: Boolean = false
}
