package at.hannibal2.skyhanni.config.features.mining.orderedwaypoints

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OrderedWaypointsConfig {
    @Expose
    @ConfigOption(name = "Current Color", desc = "Color of the current ordered point.")
    @ConfigEditorColour
    var currentWaypointColor: String = "0:153:85:255:85"

    @Expose
    @ConfigOption(name = "Previous Color", desc = "Color of the previous ordered point.")
    @ConfigEditorColour
    var previousWaypointColor: String = "0:153:85:85:255"

    @Expose
    @ConfigOption(name = "Next Color", desc = "Color of the next ordered point.")
    @ConfigEditorColour
    var nextWaypointColor: String = "0:153:255:255:88"

    @Expose
    @ConfigOption(name = "Waypoint Range", desc = "How close you have to be for it to go to the next waypoint.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.1f)
    var waypointRange: Float = 3f

    @Expose
    @ConfigOption(name = "Enable trace line", desc = "Enables the trace line.")
    @ConfigEditorBoolean
    @FeatureToggle
    var traceLine: Boolean = true

    @Expose
    @ConfigOption(name = "Trace Line Color", desc = "Color of the trace line.")
    @ConfigEditorColour
    var traceLineColor: String = "0:255:85:255:85"

    @Expose
    @ConfigOption(name = "Trace Line Thickness", desc = "Thickness of the trace line.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var traceLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(name = "Show Distance", desc = "Whether the distance for ordered waypoints should be shown.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showDistance: Boolean = true

    @Expose
    @ConfigOption(name = "Setup mode", desc = "Setup mode for route clearing.")
    @ConfigEditorBoolean
    @FeatureToggle
    var setupMode: Boolean = false

    @Expose
    @ConfigOption(name = "Setup Mode Line Thickness", desc = "Thickness of the setup mode lines.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var setupModeLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Whether all waypoints should be displayed. May cause lag")
    @ConfigEditorBoolean
    @FeatureToggle
    var showAll: Boolean = false
}
