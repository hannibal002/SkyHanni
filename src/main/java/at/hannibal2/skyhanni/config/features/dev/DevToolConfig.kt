package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DevToolConfig {
    @Expose
    @ConfigOption(name = "Graph Tools", desc = "")
    @Accordion
    val graph: GraphConfig = GraphConfig()

    @Expose
    @ConfigOption(name = "Face Test", desc = "")
    @Accordion
    val canSeeFace: TestCanSeeFaceConfig = TestCanSeeFaceConfig()

    class TestCanSeeFaceConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable the /shtestcanseeface command.")
        @ConfigEditorBoolean
        var enabled: Boolean = false

        @ConfigOption(
            name = "Use",
            desc = "All of these options apply to use of the /shtestcanseeface command.\n" +
                "See KDocs in LocationUtils for more param info."
        )
        @ConfigEditorInfoText
        @Suppress("StorageVarOrVal")
        val note: String = ""

        @Expose
        @ConfigOption(name = "Step Count", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var stepCount: Int = 0

        @Expose
        @ConfigOption(name = "Step Density", desc = "")
        @ConfigEditorSlider(minValue = 4f, maxValue = 100f, minStep = 1f)
        var stepDensity: Int = 4

        @Expose
        @ConfigOption(name = "Draw Rays", desc = "Render color-coded rays from points that are being checked.")
        @ConfigEditorBoolean
        var drawPoints: Boolean = true

        @Expose
        @ConfigOption(name = "Ray Length", desc = "How long the rays should be drawn (in blocks).")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5f, minStep = 0.1f)
        var rayLength: Float = 0.5f

        @Expose
        @ConfigOption(name = "Ray Thickness", desc = "How thick the rays should be drawn (in blocks).")
        @ConfigEditorSlider(minValue = 0.01f, maxValue = 0.1f, minStep = 0.01f)
        var rayThickness: Float = 0.02f

        @Expose
        @ConfigOption(name = "Highlight Faces", desc = "Highlight entire faces that are being checked.")
        @ConfigEditorBoolean
        var highlightFaces: Boolean = true

        @Expose
        @ConfigOption(name = "Refresh Interval", desc = "How often to refresh the face check (in seconds).")
        @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
        var refreshInterval: Int = 5

        @Expose
        @ConfigOption(name = "Debug Info", desc = "Render debug info about the face check.")
        @ConfigEditorBoolean
        var debugInfo: Boolean = false

        @Expose
        @ConfigOption(name = "Vectors Per Face", desc = "How many vectors should be displayed per face when debug info is enabled.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
        var vectorsPerFace: Int = 5

        @Expose
        @ConfigLink(owner = TestCanSeeFaceConfig::class, field = "debugInfo")
        val debugPosition: Position = Position(100, 100)
    }
}
