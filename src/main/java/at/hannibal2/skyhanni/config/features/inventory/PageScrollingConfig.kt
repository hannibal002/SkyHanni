package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.ConfigEditorKeymapping
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PageScrollingConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Enables you to scroll in any inventory with multiple pages.")
    @ConfigEditorBoolean
    var enable: Boolean = false

    @Expose
    @ConfigOption(
        name = "Bypass Key",
        desc = "When the key is held allows you to scroll even though you are over an item."
    )
    @ConfigEditorKeymapping(defaultKey = KEY_LSHIFT)
    var bypassKey = InputCode.KEY_LSHIFT

    @Expose
    @ConfigOption(
        name = "Invert Bypass",
        desc = "Inverts the behaviour of the bypass key. With this option the" +
            " bypass key blocks scrolling over items instead of allowing it."
    )
    @ConfigEditorBoolean
    var invertBypass: Boolean = false

    @Expose
    @ConfigOption(name = "Invert Scroll", desc = "Inverts the direction of the scrolling.")
    @ConfigEditorBoolean
    var invertScroll: Boolean = false
}
