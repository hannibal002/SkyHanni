import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class TestCanSeeFaceConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the /shtestcanseeface command.")
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(false)

    @ConfigOption(
        name = "Use",
        desc = "All of these options apply to use of the /shtestcanseeface command.\n" +
            "See KDocs in LocationUtils for more param info."
    )
    @ConfigEditorInfoText
    @Suppress("StorageVarOrVal")
    @Transient
    val note: String = ""

    @Expose
    @ConfigOption(name = "Step Count", desc = "")
    @ConfigEditorSlider(minValue = 0f, maxValue = 40f, minStep = 1f)
    val stepCount: Property<Int> = Property.of(0)

    @Expose
    @ConfigOption(name = "Step Density", desc = "")
    @ConfigEditorSlider(minValue = 4f, maxValue = 40f, minStep = 1f)
    val stepDensity: Property<Int> = Property.of(4)

    @Expose
    @ConfigOption(name = "Draw Rays", desc = "Render color-coded rays from points that are being checked.")
    @ConfigEditorBoolean
    val drawPoints: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Ray Length", desc = "How long the rays should be drawn (in blocks).")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 5f, minStep = 0.1f)
    val rayLength: Property<Float> = Property.of(0.5f)

    @Expose
    @ConfigOption(name = "Ray Thickness", desc = "How thick the rays should be drawn (in blocks).")
    @ConfigEditorSlider(minValue = 0.01f, maxValue = 0.1f, minStep = 0.01f)
    val rayThickness: Property<Float> = Property.of(0.02f)

    @Expose
    @ConfigOption(name = "Highlight Faces", desc = "Highlight entire faces that are being checked.")
    @ConfigEditorBoolean
    val highlightFaces: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Refresh Interval", desc = "How often to refresh the face check (in seconds).")
    @ConfigEditorSlider(minValue = 1f, maxValue = 120f, minStep = 1f)
    val refreshInterval: Property<Int> = Property.of(5)

    @Expose
    @ConfigOption(name = "Refresh on Move", desc = "Refresh the face check when the player moves.")
    @ConfigEditorBoolean
    val refreshOnMove: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Debug Info", desc = "Render debug info about the face check.")
    @ConfigEditorBoolean
    val debugInfo: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Vectors Per Face", desc = "How many vectors should be displayed per face when debug info is enabled.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    val vectorsPerFace: Property<Int> = Property.of(4)

    @Expose
    @ConfigLink(owner = TestCanSeeFaceConfig::class, field = "debugInfo")
    val debugPosition: Position = Position(100, 100)
}
