package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.InputKeys
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_1)
    var slot1 = InputKeys.KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for loadout slot 2.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_2)
    var slot2 = InputKeys.KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for loadout slot 3.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_3)
    var slot3 = InputKeys.KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for loadout slot 4.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_4)
    var slot4 = InputKeys.KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for loadout slot 5.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_5)
    var slot5 = InputKeys.KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for loadout slot 6.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_6)
    var slot6 = InputKeys.KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for loadout slot 7.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_7)
    var slot7 = InputKeys.KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for loadout slot 8.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_8)
    var slot8 = InputKeys.KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for loadout slot 9.")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_9)
    var slot9 = InputKeys.KEY_9

    @Expose
    @ConfigOption(name = "Slot 10", desc = "Keybind for loadout slot 10.")
    @ConfigEditorKeyMapping
    var slot10 = InputConstants.UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 11", desc = "Keybind for loadout slot 11.")
    @ConfigEditorKeyMapping
    var slot11 = InputConstants.UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 12", desc = "Keybind for loadout slot 12.")
    @ConfigEditorKeyMapping
    var slot12 = InputConstants.UNKNOWN
}
