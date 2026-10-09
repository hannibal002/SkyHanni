package at.hannibal2.skyhanni.config.generic

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

open class LineToConfig(
    defaultOn: Boolean = false,
    defaultWidth: Int = 3,
    defaultColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(255),
) {
    @Expose
    @ConfigOption(name = "Toggle Line", desc = "Draw a Line to Crosshair from Target.")
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
    var color: ChromaColour = defaultColor
    // This Color should be redefined using a class per Feature, defaults to Yellow Color since it's the SkyHanni Color.
    /*
    If you do not redefine this using a class a config fix which adds values i.e. moves a width & enabled toggle but does not move a color
    gson will create the color with no arguments & create a Yellow line.
     */

}
