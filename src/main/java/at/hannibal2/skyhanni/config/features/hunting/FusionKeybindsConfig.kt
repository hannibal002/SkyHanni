package at.hannibal2.skyhanni.config.features.hunting

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class FusionKeybindsConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "The keybinds below must be different, and won't work if you hold both at the same time.",
    )
    @SearchTag("fusion hunting box")
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Repeat Fusion Keybind", desc = "Keybind to repeat the previous fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    val repeatFusionKeybind: Property<Int> = Property.of(GLFW.GLFW_KEY_UNKNOWN)

    @Expose
    @ConfigOption(name = "Confirm Fusion Keybind", desc = "Keybind to confirm the current fusion.")
    @SearchTag("hunting box")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    val confirmFusionKeybind: Property<Int> = Property.of(GLFW.GLFW_KEY_UNKNOWN)
}
