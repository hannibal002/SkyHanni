package at.hannibal2.skyhanni.config.features.dev

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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
        @ConfigOption(name = "Draw Points", desc = "Render color-coded points that are being checked.")
        @ConfigEditorBoolean
        var drawPoints: Boolean = true

        @Expose
        @ConfigOption(name = "Highlight Faces", desc = "Highlight entire faces that are being checked.")
        @ConfigEditorBoolean
        var highlightFaces: Boolean = true
    }
}
