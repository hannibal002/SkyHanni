package at.hannibal2.skyhanni.config.features.inventory.customloadout

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class LoadoutSlotKeybindConfig(
    useNumberKeyDefaults: Boolean = false,
) {
    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for loadout slot 1.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot1: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_1 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for loadout slot 2.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot2: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_2 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for loadout slot 3.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot3: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_3 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for loadout slot 4.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot4: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_4 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for loadout slot 5.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot5: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_5 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for loadout slot 6.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot6: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_6 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for loadout slot 7.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot7: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_7 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for loadout slot 8.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot8: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_8 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for loadout slot 9.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot9: Int = if (useNumberKeyDefaults) GLFW.GLFW_KEY_9 else GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 10", desc = "Keybind for loadout slot 10.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot10: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 11", desc = "Keybind for loadout slot 11.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot11: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 12", desc = "Keybind for loadout slot 12.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var slot12: Int = GLFW.GLFW_KEY_UNKNOWN

    fun asList(): List<Int> = listOf(
        slot1,
        slot2,
        slot3,
        slot4,
        slot5,
        slot6,
        slot7,
        slot8,
        slot9,
        slot10,
        slot11,
        slot12,
    )
}
