package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LoadoutKeybindConfig {

    @Expose
    @ConfigOption(
        name = "Slot Keybinds Toggle",
        desc = "Enable/Disable the loadout slot keybinds.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var slotKeybindsToggle: Boolean = false

    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for loadout slot 1.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_1)
    var slot1: Int = InputConstants.KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for loadout slot 2.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_2)
    var slot2: Int = InputConstants.KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for loadout slot 3.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_3)
    var slot3: Int = InputConstants.KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for loadout slot 4.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_4)
    var slot4: Int = InputConstants.KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for loadout slot 5.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_5)
    var slot5: Int = InputConstants.KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for loadout slot 6.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_6)
    var slot6: Int = InputConstants.KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for loadout slot 7.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_7)
    var slot7: Int = InputConstants.KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for loadout slot 8.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_8)
    var slot8: Int = InputConstants.KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for loadout slot 9.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_9)
    var slot9: Int = InputConstants.KEY_9

    @Expose
    @ConfigOption(name = "Slot 10", desc = "Keybind for loadout slot 10.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var slot10: Int = KeyboardManager.KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 11", desc = "Keybind for loadout slot 11.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var slot11: Int = KeyboardManager.KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 12", desc = "Keybind for loadout slot 12.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var slot12: Int = KeyboardManager.KEY_UNKNOWN
}
