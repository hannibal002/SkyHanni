package at.hannibal2.skyhanni.config.generic

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class LineToConfig(
    defaultOn: Boolean = false,
    defaultWidth: Int = 3,
    defaultColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(255),
) {
    @Expose
    @ConfigOption(name = "Toggle Line", desc = "Show a Line to Eye.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showLine: Boolean = defaultOn

    @Expose
    @ConfigOption(name = "Line Width", desc = "Width of the Line.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    var lineWidth: Int = defaultWidth

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the Line.")
    @ConfigEditorColour
    val color: ChromaColour = defaultColor
    // This Color should be redefined per Feature, defaults to Yellow Color since it's the Skyhanni Color I, guess.
}
