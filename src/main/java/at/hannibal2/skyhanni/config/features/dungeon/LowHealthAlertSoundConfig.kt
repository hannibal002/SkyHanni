package at.hannibal2.skyhanni.config.features.dungeon

//#if MC < 1.21
import at.hannibal2.skyhanni.features.dungeon.LowHealthAlert
//#endif
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class LowHealthAlertSoundConfig {
    @Expose
    @ConfigOption(name = "Alert Sound", desc = "The sound that plays for the alert.")
    @ConfigEditorText
    var alertSound: String = "random.anvil_land"

    @Expose
    @ConfigOption(name = "Pitch", desc = "The pitch of the alert sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 1f

    //#if MC < 1.21
    @ConfigOption(name = "Test Sound", desc = "Test current sound settings.")
    @ConfigEditorButton(buttonText = "Test")
    var testSound: Runnable = Runnable(LowHealthAlert::playTestSound)
    //#endif

    @Expose
    @ConfigOption(name = "Repeat Sound", desc = "How many times the sound should be repeated.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var repeatSound: Int = 5

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    var sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
