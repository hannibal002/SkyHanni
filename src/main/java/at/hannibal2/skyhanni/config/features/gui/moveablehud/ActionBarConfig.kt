package at.hannibal2.skyhanni.config.features.gui.moveablehud

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ActionBarConfig : MoveableHudConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Allows for moving and scaling the action bar in the SkyHanni GUI Editor.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = ActionBarConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Show Outside SkyBlock", desc = "Shows the action bar outside of SkyBlock.")
    @ConfigEditorBoolean
    override var showOutsideSkyblock: Boolean = false
}
