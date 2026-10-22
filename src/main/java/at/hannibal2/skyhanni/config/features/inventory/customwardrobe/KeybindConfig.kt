package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.utils.InputKeys
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_1)
    var slot1 = InputKeys.KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for slot 2")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_2)
    var slot2 = InputKeys.KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for slot 3")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_3)
    var slot3 = InputKeys.KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for slot 4")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_4)
    var slot4 = InputKeys.KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for slot 5")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_5)
    var slot5 = InputKeys.KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for slot 6")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_6)
    var slot6 = InputKeys.KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for slot 7")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_7)
    var slot7 = InputKeys.KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for slot 8")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_8)
    var slot8 = InputKeys.KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for slot 9")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_9)
    var slot9 = InputKeys.KEY_9
}
