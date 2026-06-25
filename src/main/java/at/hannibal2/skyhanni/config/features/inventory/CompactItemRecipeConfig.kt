package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class CompactItemRecipeConfig {
    @Expose
    @ConfigOption(
        name = "Keybind",
        desc = "Keybind to open the recipe for the compacted form of the item you're hovering over."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN
}