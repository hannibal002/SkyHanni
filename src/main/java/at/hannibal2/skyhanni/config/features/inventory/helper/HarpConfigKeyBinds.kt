package at.hannibal2.skyhanni.config.features.inventory.helper

import at.hannibal2.skyhanni.config.ConfigEditorKeymapping
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HarpConfigKeyBinds {
    @Expose
    @ConfigOption(name = "Key 1", desc = "Key for the first node")
    @ConfigEditorKeymapping(defaultKey = KEY_1)
    var key1 = InputCode.KEY_1

    @Expose
    @ConfigOption(name = "Key 2", desc = "Key for the second node")
    @ConfigEditorKeymapping(defaultKey = KEY_2)
    var key2 = InputCode.KEY_2

    @Expose
    @ConfigOption(name = "Key 3", desc = "Key for the third node")
    @ConfigEditorKeymapping(defaultKey = KEY_3)
    var key3 = InputCode.KEY_3

    @Expose
    @ConfigOption(name = "Key 4", desc = "Key for the fourth node")
    @ConfigEditorKeymapping(defaultKey = KEY_4)
    var key4 = InputCode.KEY_4

    @Expose
    @ConfigOption(name = "Key 5", desc = "Key for the fifth node")
    @ConfigEditorKeymapping(defaultKey = KEY_5)
    var key5 = InputCode.KEY_5

    @Expose
    @ConfigOption(name = "Key 6", desc = "Key for the sixth node")
    @ConfigEditorKeymapping(defaultKey = KEY_6)
    var key6 = InputCode.KEY_6

    @Expose
    @ConfigOption(name = "Key 7", desc = "Key for the seventh node")
    @ConfigEditorKeymapping(defaultKey = KEY_7)
    var key7 = InputCode.KEY_7
}
