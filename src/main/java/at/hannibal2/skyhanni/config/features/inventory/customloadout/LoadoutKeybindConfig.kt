package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

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
    @ConfigOption(
        name = "Cycle Key",
        desc = "Cycles through the ordered loadouts below. The loadout menu must be closed before cycling again.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var cycleKey: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Cycle Order", desc = "The normal loadout cycle order.")
    @ConfigEditorDraggableList
    val cycleOrder: MutableList<CycleLoadout> = mutableListOf()

    @Expose
    @ConfigOption(
        name = "Contest Cycle Order",
        desc = "The loadout cycle order used automatically during a Jacob's Contest. Uses the normal order when empty.",
    )
    @ConfigEditorDraggableList
    val contestCycleOrder: MutableList<CycleLoadout> = mutableListOf()

    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for loadout slot 1.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
    var slot1: Int = GLFW.GLFW_KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for loadout slot 2.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
    var slot2: Int = GLFW.GLFW_KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for loadout slot 3.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
    var slot3: Int = GLFW.GLFW_KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for loadout slot 4.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
    var slot4: Int = GLFW.GLFW_KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for loadout slot 5.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5)
    var slot5: Int = GLFW.GLFW_KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for loadout slot 6.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6)
    var slot6: Int = GLFW.GLFW_KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for loadout slot 7.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7)
    var slot7: Int = GLFW.GLFW_KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for loadout slot 8.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8)
    var slot8: Int = GLFW.GLFW_KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for loadout slot 9.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9)
    var slot9: Int = GLFW.GLFW_KEY_9

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

    @Expose
    @ConfigOption(name = "Contest Slot 1", desc = "Alternate keybind for loadout slot 1 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot1: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 2", desc = "Alternate keybind for loadout slot 2 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot2: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 3", desc = "Alternate keybind for loadout slot 3 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot3: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 4", desc = "Alternate keybind for loadout slot 4 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot4: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 5", desc = "Alternate keybind for loadout slot 5 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot5: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 6", desc = "Alternate keybind for loadout slot 6 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot6: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 7", desc = "Alternate keybind for loadout slot 7 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot7: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 8", desc = "Alternate keybind for loadout slot 8 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot8: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 9", desc = "Alternate keybind for loadout slot 9 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot9: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 10", desc = "Alternate keybind for loadout slot 10 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot10: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 11", desc = "Alternate keybind for loadout slot 11 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot11: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Contest Slot 12", desc = "Alternate keybind for loadout slot 12 during a Jacob's Contest.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var contestSlot12: Int = GLFW.GLFW_KEY_UNKNOWN

    enum class CycleLoadout(private val displayName: String) {
        LOADOUT_1("Loadout 1"),
        LOADOUT_2("Loadout 2"),
        LOADOUT_3("Loadout 3"),
        LOADOUT_4("Loadout 4"),
        LOADOUT_5("Loadout 5"),
        LOADOUT_6("Loadout 6"),
        LOADOUT_7("Loadout 7"),
        LOADOUT_8("Loadout 8"),
        LOADOUT_9("Loadout 9"),
        LOADOUT_10("Loadout 10"),
        LOADOUT_11("Loadout 11"),
        LOADOUT_12("Loadout 12"),
        ;

        override fun toString() = displayName
    }
}
