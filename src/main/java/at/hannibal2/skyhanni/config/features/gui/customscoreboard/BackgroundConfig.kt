package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.OSUtils.openBrowser
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.awt.Color

class BackgroundConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a background behind the scoreboard.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Background Color", desc = "The color of the background.")
    @ConfigEditorColour
    var color: ChromaColour = Color.BLACK.toChromaColor(80)

    @Expose
    @ConfigOption(name = "Background Border Size", desc = "The size of the border around the background.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
    var borderSize: Int = 5

    @Expose
    @ConfigOption(name = "Rounded Corner Smoothness", desc = "The smoothness of the rounded corners.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var roundedCornerSmoothness: Int = 10

    @Expose
    @ConfigOption(name = "Background Outline", desc = "")
    @Accordion
    val outline: BackgroundOutlineConfig = BackgroundOutlineConfig()

    @Expose
    @ConfigOption(name = "Custom Background Image", desc = "See below on how to add your own custom background.")
    @ConfigEditorBoolean
    var useCustomBackgroundImage: Boolean = false

    @Expose
    @ConfigOption(name = "Background Image Opacity", desc = "The opacity of the custom background image.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var customBackgroundImageOpacity: Int = 100

    @ConfigOption(
        name = "Pack Creator",
        desc = "Click here to open the background creator. " +
            "You can use this website to add your own image into as your Scoreboard Background."
    )
    @ConfigEditorButton(buttonText = "Create")
    val runnable: Runnable = Runnable { openBrowser("https://j10a1n15.github.io/j10a1n15/pages/background.html") }
}
