package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestBuffWarningSoundConfig {
    @Expose
    @ConfigOption(name = "Notification Sound", desc = "The sound played for the notification.")
    @ConfigEditorText
    var name: String = "fireworks.blast"

    @Expose
    @ConfigOption(name = "Pitch", desc = "The pitch of the notification sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 0.5f

    @ConfigOption(name = "Test Sound", desc = "Test current sound settings.")
    @ConfigEditorButton(buttonText = "Test")
    val testSound: Runnable = Runnable(FarmingFortuneDisplay::playUserSound)

    @ConfigOption(name = "List of Sounds", desc = "A list of available sounds.")
    @ConfigEditorButton(buttonText = "Open")
    val listOfSounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
