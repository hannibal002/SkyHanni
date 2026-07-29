package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestHighlightsConfig {

    @Expose
    @ConfigOption(name = "Pest Highlight", desc = "Highlights visible pests with an outline.")
    @ConfigEditorBoolean
    @FeatureToggle
    var pestHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "Color of the pest outline.")
    @ConfigEditorColour
    var highlightColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 255)

    @Expose
    @ConfigOption(
        name = "Shortest Pest Route",
        desc = "Shows the shortest route from you through every visible pest.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shortestPestRoute: Boolean = false

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the shortest pest route.")
    @ConfigEditorColour
    var lineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 255)

    @Expose
    @ConfigOption(name = "Line Width", desc = "Thickness of the shortest pest route.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    var lineWidth: Int = 3

    @Expose
    @ConfigOption(
        name = "Etherwarp Pest Target",
        desc = "Suggests a reachable Etherwarp landing block near the largest group of visible pests. " +
            "You must aim, sneak, and right-click manually.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var etherwarpPestTarget: Boolean = false
}
