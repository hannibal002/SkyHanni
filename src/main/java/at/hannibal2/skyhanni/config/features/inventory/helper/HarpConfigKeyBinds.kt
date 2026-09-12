package at.hannibal2.skyhanni.config.features.inventory.helper

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.ConfigKeybind
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HarpConfigKeyBinds {
    @Expose
    @ConfigOption(name = "Key 1", desc = "Key for the first node")
    @ConfigEditorKeyMapping(defaultKey = KEY_1)
    var key1 = ConfigKeybind(KEY_1)

    @Expose
    @ConfigOption(name = "Key 2", desc = "Key for the second node")
    @ConfigEditorKeyMapping(defaultKey = KEY_2)
    var key2 = ConfigKeybind(KEY_2)

    @Expose
    @ConfigOption(name = "Key 3", desc = "Key for the third node")
    @ConfigEditorKeyMapping(defaultKey = KEY_3)
    var key3 = ConfigKeybind(KEY_3)

    @Expose
    @ConfigOption(name = "Key 4", desc = "Key for the fourth node")
    @ConfigEditorKeyMapping(defaultKey = KEY_4)
    var key4 = ConfigKeybind(KEY_4)

    @Expose
    @ConfigOption(name = "Key 5", desc = "Key for the fifth node")
    @ConfigEditorKeyMapping(defaultKey = KEY_5)
    var key5 = ConfigKeybind(KEY_5)

    @Expose
    @ConfigOption(name = "Key 6", desc = "Key for the sixth node")
    @ConfigEditorKeyMapping(defaultKey = KEY_6)
    var key6 = ConfigKeybind(KEY_6)

    @Expose
    @ConfigOption(name = "Key 7", desc = "Key for the seventh node")
    @ConfigEditorKeyMapping(defaultKey = KEY_7)
    var key7 = ConfigKeybind(KEY_7)
}
