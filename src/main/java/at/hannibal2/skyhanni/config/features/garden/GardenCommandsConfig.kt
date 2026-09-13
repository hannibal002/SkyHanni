package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.ConfigEditorKeymapping
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenCommandsConfig {
    @Expose
    @ConfigOption(
        name = "Warp Commands",
        desc = "Enable commands §e/home§7, §e/barn §7and §e/tp <plot>§7. §cOnly works while in the garden."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var warpCommands: Boolean = true

    @Expose
    @ConfigOption(
        name = "Spawn Hotkey",
        desc = "Press this key to teleport you to your Garden spawn. §cOnly works while in the garden.",
    )
    @ConfigEditorKeymapping
    var homeHotkey = InputCode.UNKNOWN

    @Expose
    @ConfigOption(
        name = "Set Spawn Hotkey",
        desc = "Press this key to set your Garden spawn. §cOnly works while in the garden.",
    )
    @ConfigEditorKeymapping
    var sethomeHotkey = InputCode.UNKNOWN

    @Expose
    @ConfigOption(
        name = "Barn Hotkey",
        desc = "Press this key to teleport you to the Garden barn. §cOnly works while in the garden."
    )
    @ConfigEditorKeymapping
    var barnHotkey = InputCode.UNKNOWN
}
