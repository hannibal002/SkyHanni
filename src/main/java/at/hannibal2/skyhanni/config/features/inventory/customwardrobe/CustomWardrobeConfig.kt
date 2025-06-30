package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class CustomWardrobeConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Custom Wardrobe GUI.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Follow mouse", desc = "Whether the \"players\" follow the movement of the mouse.")
    @ConfigEditorBoolean
    var eyesFollowMouse: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Empty Slots", desc = "Hide wardrobe slots with no armor.")
    @ConfigEditorBoolean
    var hideEmptySlots: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Locked Slots", desc = "Hide locked wardrobe slots.")
    @ConfigEditorBoolean
    var hideLockedSlots: Boolean = false

    @Expose
    var onlyFavorites: Boolean = false

    @Expose
    @ConfigOption(name = "Estimated Value", desc = "Show a §2$ §7sign you can hover to see the wardrobe slot value.")
    @ConfigEditorBoolean
    var estimatedValue: Boolean = true

    @Expose
    @ConfigOption(
        name = "Loading text",
        desc = "Show a \"§cLoading...§7\" text when the wardrobe page hasn't fully loaded in yet."
    )
    @ConfigEditorBoolean
    var loadingText: Boolean = true

    @Expose
    @ConfigOption(
        name = "Armor Tooltip Keybind",
        desc = "Only show the lore of the item hovered when holding a keybind."
    )
    @ConfigEditorBoolean
    var showTooltipOnlyKeybind: Boolean = false

    @Expose
    @ConfigOption(name = "Tooltip Keybind", desc = "Press this key to show the item tooltip.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    var tooltipKeybind: Int = Keyboard.KEY_LSHIFT

    @Expose
    @ConfigOption(name = "Show REI Items", desc = "Enables showing the REI item list from Firmament while in the custom wardrobe.")
    @OnlyModern
    @ConfigEditorBoolean
    var showReiItems: Boolean = true

    @Expose
    @ConfigOption(name = "Colors", desc = "Change the color settings.")
    @Accordion
    @Suppress("StorageVarOrVal")
    var color: ColorConfig = ColorConfig()

    @Expose
    @ConfigOption(name = "Spacing", desc = "")
    @Accordion
    @Suppress("StorageVarOrVal")
    var spacing: SpacingConfig = SpacingConfig()

    @Expose
    @ConfigOption(name = "Keybinds", desc = "")
    @Accordion
    val keybinds: KeybindConfig = KeybindConfig()
}
