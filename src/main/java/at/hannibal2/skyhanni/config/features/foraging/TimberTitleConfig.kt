package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TimberTitleConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Shows a title when a timber activates and a tree fells instantly.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = false

    @Expose
    @ConfigOption(name = "Title Duration", desc = "The duration of the title in seconds.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
    var duration: Double = 3.0

    @Expose
    @ConfigOption(name = "Title Text", desc = "The text of the title.")
    @ConfigEditorText
    var titleText = "&&2&&lTIMBER&&r &&6&&lTree Insta-Felled!"
}
