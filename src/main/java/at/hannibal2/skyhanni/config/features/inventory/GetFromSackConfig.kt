package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class GetFromSackConfig {
    // TODO rename to queued
    @Expose
    @ConfigOption(
        name = "Queued GfS",
        desc = "If §e/gfs §7or §e/getfromsacks §7is used it queues up the commands so all items are guaranteed to be received."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var queuedGFS: Boolean = true

    // TODO rename to bazaar
    @Expose
    @ConfigOption(
        name = "Bazaar GfS",
        desc = "If you don't have enough items in sack get a prompt to buy them from bazaar."
    )
    @ConfigEditorBoolean
    var bazaarGFS: Boolean = false

    // TODO rename to superCraft
    @Expose
    @ConfigOption(
        name = "Super Craft GfS",
        desc = "Send a clickable message after supercrafting an item that grabs the item from your sacks when clicked."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var superCraftGFS: Boolean = true

    // TODO rename to defaultAmount
    @Expose
    @ConfigOption(name = "Default Amount GfS", desc = "The default amount of items used when an amount isn't provided.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 64f, minStep = 1f)
    var defaultAmountGFS: Int = 1

    @Expose
    @ConfigOption(
        name = "GfS Keybind",
        desc = "Fills your inventory with the item you are hovering over."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var keybind: Int = Keyboard.KEY_NONE
}
