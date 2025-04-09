package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class CFKeybindsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "In the Chocolate Factory, press buttons with your number row on the keyboard to upgrade the rabbits."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Key 1", desc = "Key for Rabbit Bro.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
    var key1: Int = Keyboard.KEY_1

    @Expose
    @ConfigOption(name = "Key 2", desc = "Key for Rabbit Cousin.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
    var key2: Int = Keyboard.KEY_2

    @Expose
    @ConfigOption(name = "Key 3", desc = "Key for Rabbit Sis.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
    var key3: Int = Keyboard.KEY_3

    @Expose
    @ConfigOption(name = "Key 4", desc = "Key for Rabbit Daddy.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
    var key4: Int = Keyboard.KEY_4

    @Expose
    @ConfigOption(name = "Key 5", desc = "Key for Rabbit Granny.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_5)
    var key5: Int = Keyboard.KEY_5

    @Expose
    @ConfigOption(name = "Key 6", desc = "Key for Rabbit Uncle.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_6)
    var key6: Int = Keyboard.KEY_6

    @Expose
    @ConfigOption(name = "Key 7", desc = "Key for Rabbit Dog.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_7)
    var key7: Int = Keyboard.KEY_7
}
