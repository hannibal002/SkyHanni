package at.hannibal2.skyhanni.config.features.event.diana

//#if MC < 1.21
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
//#endif
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class InquisitorSoundConfig {
    @Expose
    @ConfigOption(name = "Notification Sound", desc = "The sound played when an Inquisitor is found.")
    @ConfigEditorText
    var name: String = "random.orb"

    @Expose
    @ConfigOption(name = "Pitch", desc = "The pitch of the sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 1f

    //#if MC < 1.21
    @ConfigOption(name = "Test Sound", desc = "Test current sound settings.")
    @ConfigEditorButton(buttonText = "Test")
    var testSound: Runnable = Runnable { InquisitorWaypointShare.playUserSound() }
    //#endif

    @ConfigOption(name = "List of Sounds", desc = "A list of available sounds.")
    @ConfigEditorButton(buttonText = "Open")
    var listOfSounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
