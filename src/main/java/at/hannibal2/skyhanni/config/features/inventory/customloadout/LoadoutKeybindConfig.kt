package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
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
        desc = "Cycles through the ordered loadouts below.",
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
    @ConfigOption(name = "Slot Keybinds", desc = "Keybinds used to select loadouts.")
    @Accordion
    val slotKeybinds: LoadoutSlotKeybindConfig = LoadoutSlotKeybindConfig(useNumberKeyDefaults = true)

    @Expose
    @ConfigOption(name = "Contest Slot Keybinds", desc = "Alternate keybinds used during a Jacob's Contest.")
    @Accordion
    val contestSlotKeybinds: LoadoutSlotKeybindConfig = LoadoutSlotKeybindConfig()

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

    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) = migrateSlotKeybinds(event)

        internal fun migrateSlotKeybinds(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            for (slot in 1..12) {
                event.move(
                    143,
                    "inventory.customLoadout.keybinds.slot$slot",
                    "inventory.customLoadout.keybinds.slotKeybinds.slot$slot",
                )
                event.move(
                    143,
                    "inventory.customLoadout.keybinds.contestSlot$slot",
                    "inventory.customLoadout.keybinds.contestSlotKeybinds.slot$slot",
                )
            }
        }
    }
}
