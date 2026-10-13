package at.hannibal2.skyhanni.config.features.combat.end

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DropSoundConfig {
    @Expose
    @ConfigOption(name = "Sound", desc = "The sound that plays for a special drop.")
    @ConfigEditorText
    var sound: String = "ui.toast.challenge_complete"

    @Expose
    @ConfigOption(name = "Pitch", desc = "Pitch of the sound.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.1f)
    var pitch: Float = 1f
}
