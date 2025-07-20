package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

class FusionKeybindsConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "The keybinds below must be different, and won't work if you hold both at the same time.",
    )
    @SearchTag("fusion hunting box")
    @ConfigEditorInfoText
    @OnlyModern
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Repeat Fusion Keybind", desc = "Keybind to repeat the previous fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @OnlyModern
    var repeatFusionKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Confirm Fusion Keybind", desc = "Keybind to confirm the current fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @OnlyModern
    var confirmFusionKeybind: Int = Keyboard.KEY_NONE
}
