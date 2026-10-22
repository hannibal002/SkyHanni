package at.hannibal2.skyhanni.config.features.inventory.helper

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.utils.InputKeys
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HarpConfigKeyBinds {
    @Expose
    @ConfigOption(name = "Key 1", desc = "Key for the first node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_1)
    var key1 = InputKeys.KEY_1

    @Expose
    @ConfigOption(name = "Key 2", desc = "Key for the second node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_2)
    var key2 = InputKeys.KEY_2

    @Expose
    @ConfigOption(name = "Key 3", desc = "Key for the third node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_3)
    var key3 = InputKeys.KEY_3

    @Expose
    @ConfigOption(name = "Key 4", desc = "Key for the fourth node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_4)
    var key4 = InputKeys.KEY_4

    @Expose
    @ConfigOption(name = "Key 5", desc = "Key for the fifth node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_5)
    var key5 = InputKeys.KEY_5

    @Expose
    @ConfigOption(name = "Key 6", desc = "Key for the sixth node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_6)
    var key6 = InputKeys.KEY_6

    @Expose
    @ConfigOption(name = "Key 7", desc = "Key for the seventh node")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_7)
    var key7 = InputKeys.KEY_7
}
