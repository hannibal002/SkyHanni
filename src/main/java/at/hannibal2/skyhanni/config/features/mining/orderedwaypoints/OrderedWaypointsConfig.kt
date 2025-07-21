package at.hannibal2.skyhanni.config.features.mining.orderedwaypoints

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OrderedWaypointsConfig {

    @ConfigOption(name = "Credits", desc = "This feature is from Coleweight and SoopyV2, huge thanks to them!")
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Enable Ordered Waypoints", desc = "Enables ordered waypoints.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Current Color", desc = "Color of the current ordered waypoint.")
    @ConfigEditorColour
    var currentWaypointColor: ChromaColour = ChromaColour.fromRGB(85, 255, 85, 0, 153)

    @Expose
    @ConfigOption(name = "Previous Color", desc = "Color of the previous ordered waypoint.")
    @ConfigEditorColour
    var previousWaypointColor: ChromaColour = ChromaColour.fromRGB(85, 85, 255, 0, 153)

    @Expose
    @ConfigOption(name = "Next Color", desc = "Color of the next ordered waypoint(s).")
    @ConfigEditorColour
    var nextWaypointColor: ChromaColour = ChromaColour.fromRGB(255, 255, 85, 0, 153)

    @Expose
    @ConfigOption(name = "Next Waypoints", desc = "How many waypoints in front of the current waypoint should be rendered.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
    var nextCount: Float = 2f

    @Expose
    @ConfigOption(name = "Block Outline Thickness", desc = "Thickness of the block outline.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var blockOutlineThickness: Float = 1f

    @Expose
    @ConfigOption(name = "Fill Block", desc = "Whether the waypoints should be filled instead of just being the outline.")
    @ConfigEditorBoolean
    var fillBlock: Boolean = false

    @Expose
    @ConfigOption(name = "Waypoint Range", desc = "How close you have to be for it to go to the next waypoint.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.1f)
    var waypointRange: Float = 3f

    @Expose
    @ConfigOption(name = "Enable trace line", desc = "Enables the trace line.")
    @ConfigEditorBoolean
    var traceLine: Boolean = true

    @Expose
    @ConfigOption(name = "Trace Line Color", desc = "Color of the trace line.")
    @ConfigEditorColour
    var traceLineColor: ChromaColour = ChromaColour.fromRGB(85, 255, 85, 0, 255)

    @Expose
    @ConfigOption(name = "Trace Line Thickness", desc = "Thickness of the trace line.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var traceLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(name = "Show Distance", desc = "Whether the distance for ordered waypoints should be shown.")
    @ConfigEditorBoolean
    var showDistance: Boolean = true

    @Expose
    @ConfigOption(name = "Setup Mode", desc = "Setup mode for route clearing.")
    @ConfigEditorBoolean
    var setupMode: Boolean = false

    @Expose
    @ConfigOption(name = "Setup Mode Line Color", desc = "Line color for the setup mode lines.")
    @ConfigEditorColour
    var setupModeLineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 102)

    @Expose
    @ConfigOption(name = "Setup Mode Waypoint Color", desc = "Color used for additional waypoints displayed by setup mode.")
    @ConfigEditorColour
    var setupModeColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 102)

    @Expose
    @ConfigOption(name = "Setup Mode Range", desc = "How close you need to be for nearby waypoints to show in setup mode.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 100f, minStep = 1f)
    var setupModeRange: Float = 16f

    @Expose
    @ConfigOption(name = "Setup Mode Line Thickness", desc = "Thickness of the setup mode lines.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var setupModeLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(
        name = "Sneaking During Route",
        desc = "" +
            "Whether you'll be sneaking when moving between waypoints (e.g., using AOTV)." +
            "This is used for drawing the line of sight line for setup mode."
    )
    @ConfigEditorBoolean
    var sneakingDuringRoute: Boolean = true

    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Whether all waypoints should be displayed.")
    @ConfigEditorBoolean
    var showAll: Boolean = false

    @Expose
    @ConfigOption(name = "All Waypoint Color", desc = "Color used for waypoints when using show all mode.")
    @ConfigEditorColour
    var showAllWaypointColor: ChromaColour = ChromaColour.fromStaticRGB(0, 255, 0, 102)
}
