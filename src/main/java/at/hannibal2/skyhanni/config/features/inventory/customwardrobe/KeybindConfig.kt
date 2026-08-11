package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.config.core.elements.ConfigEditorKeyMap
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class KeybindConfig {
    @Expose
    @ConfigOption(
        name = "Slot Keybinds Toggle",
        desc = "Enable/Disable the slot keybinds.\n§cThis only works inside the Custom Wardrobe GUI."
    )
    @ConfigEditorBoolean
    var slotKeybindsToggle: Boolean = true

    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for slot 1")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_1)
    var slot1: Int = GLFW.GLFW_KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for slot 2")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_2)
    var slot2: Int = GLFW.GLFW_KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for slot 3")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_3)
    var slot3: Int = GLFW.GLFW_KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for slot 4")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_4)
    var slot4: Int = GLFW.GLFW_KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for slot 5")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_5)
    var slot5: Int = GLFW.GLFW_KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for slot 6")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_6)
    var slot6: Int = GLFW.GLFW_KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for slot 7")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_7)
    var slot7: Int = GLFW.GLFW_KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for slot 8")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_8)
    var slot8: Int = GLFW.GLFW_KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for slot 9")
    @ConfigEditorKeyMap(defaultKey = GLFW.GLFW_KEY_9)
    var slot9: Int = GLFW.GLFW_KEY_9
}
