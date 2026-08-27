package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BurrowCustomization {

    @Expose
    @ConfigOption(
        name = "Start Color",
        desc = "Color of Start Burrow waypoints and lines"
    )
    @ConfigEditorColour
    var startBurrowColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Mob Color",
        desc = "Color of Mob Burrow waypoints and lines"
    )
    @ConfigEditorColour
    var mobBurrowColor: ChromaColour = LorenzColor.RED.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Treasure Color",
        desc = "Color of Treasure Burrow waypoints and lines"
    )
    @ConfigEditorColour
    var treasureBurrowColor: ChromaColour = LorenzColor.GOLD.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Waypoints Filled",
        desc = "Whether to render Burrow Waypoints as a Full block (enabled) or Outline (Disabled)"
    )
    @ConfigEditorBoolean
    var shouldRenderAsFullBlock: Boolean = true

    @Expose
    @ConfigOption(
        name = "Outline Width",
        desc = "Width of the outline of a Burrow if it is being rendered as an outline."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var burrowOutlineWidth: Float = 1f
}
