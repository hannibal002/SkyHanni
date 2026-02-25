package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LegionBobbinOverlayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the current Legion and Bobbin' Time buff.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide without enchant", desc = "Hide the gui when you aren't wearing armor with Legion or Bobbin' on it.")
    @ConfigEditorBoolean
    var hideWithoutEnchant: Boolean = true

    @Expose
    @ConfigLink(owner = LegionBobbinOverlayConfig::class, field = "enabled")
    val position: Position = Position(100, 100)

}
