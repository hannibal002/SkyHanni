package at.hannibal2.skyhanni.config.features.event.carnival

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FruitDiggingConfig {
    @ConfigOption(name = "WIP", desc = "§cRThis feature is currently in work, this only a preview!")
    @ConfigEditorInfoText
    var earlyAccess: String? = null

    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows safe / mine spots for Fruit Digging.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Safe Colour", desc = "Colour for safe spots to be displayed in.")
    @ConfigEditorDropdown
    var safe: LorenzColor = LorenzColor.GREEN

    @Expose
    @ConfigOption(name = "Mine Colour", desc = "Colour for mine spots to be displayed in.")
    @ConfigEditorDropdown
    var mine: LorenzColor = LorenzColor.RED

    @Expose
    @ConfigOption(name = "Uncovered Colour", desc = "Colour for uncovered spots to be displayed in.")
    @ConfigEditorDropdown
    var uncovered: LorenzColor = LorenzColor.YELLOW
}
