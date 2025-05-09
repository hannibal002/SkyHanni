package at.hannibal2.skyhanni.config.features.combat.broodmother

//#if MC < 1.21
import at.hannibal2.skyhanni.features.combat.BroodmotherFeatures
//#endif
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class BroodmotherSpawnAlertConfig {
    @Expose
    @ConfigOption(name = "Alert Sound", desc = "The sound that plays for the alert.")
    @ConfigEditorText
    var alertSound: String = "note.pling"

    @Expose
    @ConfigOption(name = "Pitch", desc = "The pitch of the alert sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 1f

    //#if MC < 1.21
    @ConfigOption(name = "Test Sound", desc = "Test current sound settings.")
    @ConfigEditorButton(buttonText = "Test")
    var testSound: Runnable = Runnable(BroodmotherFeatures::playTestSound)
    //#endif

    @Expose
    @ConfigOption(name = "Repeat Sound", desc = "How many times the sound should be repeated.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var repeatSound: Int = 20

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    var sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)

    @Expose
    @ConfigOption(name = "Text", desc = "The text with color to be displayed as the title notification.")
    @ConfigEditorText
    var text: String = "&4Broodmother has spawned!"
}
