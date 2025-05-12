package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class ExperimentsAddonHelpersConfig {

    @Expose
    @ConfigOption(name = "Chronomatron Keybinds", desc = "")
    @Accordion
    var chronomatronKeybinds: ChronomatronKeybindConfig = ChronomatronKeybindConfig()

    class ChronomatronKeybindConfig {

        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Use custom keybinds for different Chronomatron colors."
        )
        @ConfigEditorBoolean
        var enabled: Boolean = false

        @Expose
        @ConfigOption(name = "§cRed Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_0)
        var redKeybind: Int = Keyboard.KEY_0

        @Expose
        @ConfigOption(name = "§9Blue Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
        var blueKeybind: Int = Keyboard.KEY_1

        @Expose
        @ConfigOption(name = "§aLime Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
        var limeKeybind: Int = Keyboard.KEY_2

        @Expose
        @ConfigOption(name = "§eYellow Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
        var yellowKeybind: Int = Keyboard.KEY_3

        @Expose
        @ConfigOption(name = "§bLight Blue Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
        var lightKeybind: Int = Keyboard.KEY_4

        @Expose
        @ConfigOption(name = "§dPink Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_5)
        var pinkKeybind: Int = Keyboard.KEY_5

        @Expose
        @ConfigOption(name = "§2Green Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_6)
        var greenKeybind: Int = Keyboard.KEY_6

        @Expose
        @ConfigOption(name = "§3Cyan Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_7)
        var cyanKeybind: Int = Keyboard.KEY_7

        // Blame Mojang for making a gold color code instead of Orange
        @Expose
        @ConfigOption(name = "§#§c§d§c§4§0§0§/Orange Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_8)
        var orangeKeybind: Int = Keyboard.KEY_8

        @Expose
        @ConfigOption(name = "§5Purple Keybind", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_9)
        var purpleKeybind: Int = Keyboard.KEY_9

    }

    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlights the next slot to click in Chronomatron and Superpairs")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Prevent Misclicks",
        desc = "Prevent clicking wrong colors in Chronomatron, and wrong slots in Superpairs."
    )
    @ConfigEditorBoolean
    var preventMisclicks: Boolean = false

}
