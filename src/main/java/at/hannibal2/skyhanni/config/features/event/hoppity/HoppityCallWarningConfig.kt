package at.hannibal2.skyhanni.config.features.event.hoppity

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class HoppityCallWarningConfig {
    @Expose
    @ConfigOption(name = "Hoppity Call Warning", desc = "Warn when hoppity is calling you.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Accept Call Hotkey", desc = "Accept the call from hoppity by pressing this keybind.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var acceptHotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Warning Sound",
        desc = "The sound that plays when hoppity calls.\n" +
            "§eYou can use custom sounds, put it in the §bskyhanni/sounds §efolder in your resource pack.\n" +
            "§eThen write §bskyhanni:yourfilename\n" +
            "§cMust be a .ogg file",
    )
    @ConfigEditorText
    var hoppityCallSound: Property<String> = Property.of("note.pling")

    @Expose
    @ConfigOption(name = "Flash Color", desc = "Color of the screen when flashing")
    @ConfigEditorColour
    var flashColor: String = "0:127:0:238:255"

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    var sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)

    @Expose
    @ConfigOption(
        name = "Ensure Coins Pre-Trade",
        desc = "Block opening Hoppity's abiphone trade menu if you do not have enough coins in your purse.",
    )
    @ConfigEditorBoolean
    var ensureCoins: Boolean = false

    @Expose
    @ConfigOption(
        name = "Coin Threshold",
        desc = "The amount of coins you need to have in your purse to be able to open Hoppity's abiphone trade menu.",
    )
    @ConfigEditorSlider(minValue = 250000f, maxValue = 5000000f, minStep = 250000f)
    var coinThreshold: Int = 5000000
}
