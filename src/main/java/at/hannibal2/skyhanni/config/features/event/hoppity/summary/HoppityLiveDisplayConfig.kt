package at.hannibal2.skyhanni.config.features.event.hoppity.summary

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class HoppityLiveDisplayConfig {
    @Expose
    @ConfigOption(name = "Show Display", desc = "Show a hoppity stats card in a GUI element.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @ConfigOption(
        name = "Note",
        desc = "§cNote§7: This card will mirror the stat list that is defined in the Hoppity Event Summary config.",
    )
    @ConfigEditorInfoText
    var mirrorConfigNote: Boolean = false

    enum class HoppityDateTimeDisplayType(private val displayName: String) {
        CURRENT("Current Event"),
        PAST_EVENTS("Past Events"),
        NEXT_EVENT("Next Event"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Date/Time Display",
        desc = "Display the date and time of the event in the header, for the current event, past events, or the next event.\n" +
            "§cNote§7: The Next Event option will only appear if Next Event is added here.",
    )
    @ConfigEditorDraggableList
    val dateTimeDisplay: MutableList<HoppityDateTimeDisplayType> = mutableListOf(
        HoppityDateTimeDisplayType.CURRENT,
    )

    enum class HoppityDateTimeFormat(private val displayName: String) {
        RELATIVE("Relative"),
        ABSOLUTE("Absolute"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Date Time Format", desc = "The format of the date and time.")
    @ConfigEditorDropdown
    var dateTimeFormat: HoppityDateTimeFormat = HoppityDateTimeFormat.RELATIVE

    @Expose
    @ConfigOption(
        name = "Show All-Time",
        desc = "Add a dummy \"All-Time\" entry after the last set of stats, showing the total stats for all recorded events.",
    )
    @ConfigEditorBoolean
    var showAllTime: Boolean = true

    @Expose
    @ConfigOption(
        name = "Meal Egg Hover",
        desc = "Hovering over number of meal eggs found will show a tooltip of which eggs were found how many times.",
    )
    @ConfigEditorBoolean
    var mealEggHover: Boolean = true

    @Expose
    @ConfigOption(name = "Card Toggle Keybind", desc = "Toggle the GUI element with this keybind.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var toggleKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Specific Inventories",
        desc = "§cOnly§r show the card while in certain inventories.\n" +
            "§eIf the list is empty, the card will show in all inventories.",
    )
    @ConfigEditorDraggableList
    val specificInventories: MutableList<HoppityLiveDisplayInventoryType> = mutableListOf(
        HoppityLiveDisplayInventoryType.NO_INVENTORY,
        HoppityLiveDisplayInventoryType.CHOCOLATE_FACTORY,
    )

    enum class HoppityLiveDisplayInventoryType(private val displayName: String) {
        NO_INVENTORY("No Inventory"),
        OWN_INVENTORY("Own Inventory"),
        CHOCOLATE_FACTORY("Chocolate Factory"),
        HOPPITY("Hoppity"),
        MEAL_EGGS("Meal Eggs"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Only During Event", desc = "§cOnly§r show the card while Hoppity's Hunt is active.")
    @ConfigEditorBoolean
    var onlyDuringEvent: Boolean = true

    @Expose
    @ConfigOption(name = "Only Holding Egglocator", desc = "§cOnly§r show the card when holding an Egglocator.")
    @ConfigEditorBoolean
    var mustHoldEggLocator: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only Hoppity Islands",
        desc = "§cOnly§r show the card while on Islands that spawn Hoppity Eggs (will not show on Garden, Island, Dungeons etc.).",
    )
    @ConfigEditorBoolean
    var onlyHoppityIslands: Boolean = false
}
