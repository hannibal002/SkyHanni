package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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
    @ConfigOption(
        name = "Very Busy Threshold",
        desc = "Lobbies at least this % full use the Very Busy color.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var veryBusyThreshold: Int = 75

    @Expose
    @ConfigOption(name = "Very Busy Color", desc = "Color for the Very Busy band.")
    @ConfigEditorColour
    var veryBusyColor: ChromaColour = LorenzColor.RED.toChromaColor(255)

    @Expose
    @ConfigOption(
        name = "Busy Threshold",
        desc = "Lobbies at least this % full use the Busy color.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var busyThreshold: Int = 50

    @Expose
    @ConfigOption(name = "Busy Color", desc = "Color for the Busy band.")
    @ConfigEditorColour
    var busyColor: ChromaColour = LorenzColor.GOLD.toChromaColor(255)

    @Expose
    @ConfigOption(
        name = "Moderate Threshold",
        desc = "Lobbies at least this % full use the Moderate color.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var moderateThreshold: Int = 25

    @Expose
    @ConfigOption(name = "Moderate Color", desc = "Color for the Moderate band.")
    @ConfigEditorColour
    var moderateColor: ChromaColour = LorenzColor.YELLOW.toChromaColor(255)

    @Expose
    @ConfigOption(
        name = "Quiet Color",
        desc = "Color for lobbies below the Moderate threshold. §eTransparent by default.",
    )
    @ConfigEditorColour
    var quietColor: ChromaColour = LorenzColor.WHITE.toChromaColor(0)
}
