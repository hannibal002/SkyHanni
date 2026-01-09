package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.features.garden.pests.PestSpawnSound
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestSpawnSoundSettings {
    @Expose
    @ConfigOption(name = "Notification Sound", desc = "The sound played for the notification.")
    @ConfigEditorText
    var name: String = "note.bassattack"

    @Expose
    @ConfigOption(name = "Pitch", desc = "The pitch of the notification sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 1.4920635f

    @Expose
    @ConfigOption(
        name = "Repeat Frequency",
        desc = "Change how often the sound should be repeated in milliseconds."
    )
    @ConfigEditorSlider(minValue = 50f, maxValue = 1000f, minStep = 50f)
    var repeatFrequency: Int = 150

    @Expose
    @ConfigOption(
        name = "Repeat Amount",
        desc = "Change the amount of times the sound should be repeated."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var repeatAmount: Int = 3

    @ConfigOption(name = "Test Sound", desc = "Test current sound settings.")
    @ConfigEditorButton(buttonText = "Test")
    val testSound: Runnable = Runnable(PestSpawnSound::repeatSpawnSound)

    @ConfigOption(name = "List of Sounds", desc = "A list of available sounds.")
    @ConfigEditorButton(buttonText = "Open")
    val listOfSounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
