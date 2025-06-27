package at.hannibal2.skyhanni.config.features.inventory.helper

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class HarpConfigKeyBinds {
    @Expose
    @ConfigOption(name = "Key 1", desc = "Key for the first node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
    var key1: Int = Keyboard.KEY_1

    @Expose
    @ConfigOption(name = "Key 2", desc = "Key for the second node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
    var key2: Int = Keyboard.KEY_2

    @Expose
    @ConfigOption(name = "Key 3", desc = "Key for the third node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
    var key3: Int = Keyboard.KEY_3

    @Expose
    @ConfigOption(name = "Key 4", desc = "Key for the fourth node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
    var key4: Int = Keyboard.KEY_4

    @Expose
    @ConfigOption(name = "Key 5", desc = "Key for the fifth node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_5)
    var key5: Int = Keyboard.KEY_5

    @Expose
    @ConfigOption(name = "Key 6", desc = "Key for the sixth node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_6)
    var key6: Int = Keyboard.KEY_6

    @Expose
    @ConfigOption(name = "Key 7", desc = "Key for the seventh node")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_7)
    var key7: Int = Keyboard.KEY_7
}
