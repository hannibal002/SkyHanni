package at.hannibal2.skyhanni.config.features.inventory

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class CompactItemRecipeConfig {
    @Expose
    @ConfigOption(
        name = "Keybind",
        desc = "Keybind to open the recipe for the compacted/enchanted form of the item you're hovering over. " +
            "§eUseful for quickly Supercrafting from sacks!",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Forge Recipe Action",
        desc = "Select which action to perform when clicking on the message for a Forge recipe.",
    )
    @ConfigEditorDropdown
    var forgeRecipeAction: ForgeRecipeAction = ForgeRecipeAction.NONE

    enum class ForgeRecipeAction(private val displayName: String) {
        NONE("None"),
        WARP_FORGE("Warp to Forge"),
        CALL_FRED("Call Fred"),
        ;

        override fun toString() = displayName
    }
}
