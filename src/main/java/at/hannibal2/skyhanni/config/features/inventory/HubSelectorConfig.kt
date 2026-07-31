package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HubSelectorConfig {
    @Expose
    @ConfigOption(
        name = "Highlight Lobbies",
        desc = "Highlight hub lobbies in the hub selector, colored by how full they are.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Very Busy Color (45+)", desc = "Color for lobbies with 45 or more players.")
    @ConfigEditorColour
    var veryBusyColor: ChromaColour = LorenzColor.RED.toChromaColor(255)

    @Expose
    @ConfigOption(name = "Busy Color (30+)", desc = "Color for lobbies with 30 to 44 players.")
    @ConfigEditorColour
    var busyColor: ChromaColour = LorenzColor.GOLD.toChromaColor(255)

    @Expose
    @ConfigOption(name = "Moderate Color (15+)", desc = "Color for lobbies with 15 to 29 players.")
    @ConfigEditorColour
    var moderateColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(255)

    @Expose
    @ConfigOption(name = "Quiet Color (under 15)", desc = "Color for lobbies with fewer than 15 players. §eTransparent by default.")
    @ConfigEditorColour
    var quietColor: ChromaColour = LorenzColor.WHITE.toChromaColor(0)
}
