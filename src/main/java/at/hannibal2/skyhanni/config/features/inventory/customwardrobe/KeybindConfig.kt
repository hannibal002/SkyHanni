package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_1)
    var slot1: Int = InputConstants.KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for slot 2")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_2)
    var slot2: Int = InputConstants.KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for slot 3")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_3)
    var slot3: Int = InputConstants.KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for slot 4")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_4)
    var slot4: Int = InputConstants.KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for slot 5")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_5)
    var slot5: Int = InputConstants.KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for slot 6")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_6)
    var slot6: Int = InputConstants.KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for slot 7")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_7)
    var slot7: Int = InputConstants.KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for slot 8")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_8)
    var slot8: Int = InputConstants.KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for slot 9")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_9)
    var slot9: Int = InputConstants.KEY_9
}
