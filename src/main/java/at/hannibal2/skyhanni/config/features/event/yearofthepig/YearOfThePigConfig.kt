package at.hannibal2.skyhanni.config.features.event.yearofthepig

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class YearOfThePigConfig {

    @Expose
    @Accordion
    @ConfigOption(name = "Shiny Orb Tracker", desc = "")
    val shinyOrbTracker = ShinyOrbTrackerConfig()

    @Expose
    @ConfigOption(name = "Lines to Draw", desc = "Which helper lines to draw.")
    @ConfigEditorDraggableList
    val linesToDraw: MutableList<ShinyOrbLineType> = mutableListOf(
        ShinyOrbLineType.TO_PIG,
        ShinyOrbLineType.TO_ORB,
    )

    enum class ShinyOrbLineType(private val displayName: String) {
        TO_PIG("§dYou to Pig"),
        TO_ORB("§6Pig to Orb"),
        ;

        override fun toString() = displayName
    }
}
