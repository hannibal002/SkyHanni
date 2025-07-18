package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

class FocusModeConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "In focus mode you only see the name of the item instead of the whole description. §eSet a Toggle key below to use."
    )
    @SearchTag("compact hide")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Toggle Key", desc = "Key to toggle the focus mode on and off.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var toggleKey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Disable Hint",
        desc = "Disable the line in item tooltips that shows how to enable or disable this feature via key press."
    )
    @ConfigEditorBoolean
    var disableHint: Boolean = false

    @Expose
    @ConfigOption(name = "Always Enabled", desc = "Ignore the keybind and enable this feature all the time.")
    @ConfigEditorBoolean
    var alwaysEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Menu Items", desc = "Also hide the lore of non-SkyBlock items in menus.")
    @ConfigEditorBoolean
    var hideMenuItems: Boolean = true
}
