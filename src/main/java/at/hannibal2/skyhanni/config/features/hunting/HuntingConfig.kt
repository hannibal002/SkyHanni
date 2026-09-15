package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.foraging.SafariConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.glfw.GLFW

class HuntingConfig {

    @Expose
    @Category(name = "Safari", desc = "Settings for the safari")
    val safari = SafariConfig()

    @Expose
    @ConfigOption(name = "Shard Tracker", desc = "")
    @Accordion
    val shardTracker: ShardTrackerConfig = ShardTrackerConfig()

    @Expose
    @ConfigOption(name = "Hunting Profit Tracker", desc = "")
    @Accordion
    val huntingProfitTracker: HuntingProfitTrackerConfig = HuntingProfitTrackerConfig()

    @Expose
    @Category(name = "Galatea Mob Highlights", desc = "Settings for Galatea mob highlights")
    val mobHighlight: GalateaMobHighlightConfig = GalateaMobHighlightConfig()

    @Expose
    @ConfigOption(name = "Lasso Display", desc = "Displays your lasso progress on screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lassoDisplay = true

    @Expose
    @ConfigLink(owner = HuntingConfig::class, field = "lassoDisplay")
    val lassoDisplayPosition: Position = Position(380, 210)

    @Expose
    @ConfigOption(name = "Fusion Keybinds", desc = "")
    @SearchTag("hunting box")
    @Accordion
    val fusionKeybinds = FusionKeybindsConfig()

    @Expose
    @ConfigOption(
        name = "Prevent Huntrap Misclick",
        desc = "Prevents clicking on empty traps in the Hunting Toolkit.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var huntrapMisclick = false

    @Expose
    @ConfigOption(
        name = "Shulker Finder",
        desc = "Shows a route from your position to the nearest possible spawn point for a Shulker for easy hunting.",
    )
    @SearchTag("hideonleaf hideonsun")
    @ConfigEditorBoolean
    @FeatureToggle
    var shulkerFinder = false

    @Expose
    @ConfigOption(name = "Show next Shulker", desc = "Press this key to show the route to the next Shulker.")
    @SearchTag("hideonleaf hideonsun")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var nextShulkerKeybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Fusion Display", desc = "Displays the shard you are fusing and how many you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fusionDisplay = true

    @Expose
    @ConfigLink(owner = HuntingConfig::class, field = "fusionDisplay")
    val fusionDisplayPosition: Position = Position(30, 210)

    @Expose
    @ConfigOption(
        name = "Line to Floor Drop",
        desc = "Draws a line to the nearest Floor Drop on the ground.",
    )
    @SearchTag("moonglade torrhus safari")
    @ConfigEditorBoolean
    @FeatureToggle
    var lineToFloorDrop: Boolean = true

    @Expose
    @ConfigOption(
        name = "Line to Floor Drop Width",
        desc = "The width of the line pointing to the nearest Floor Drop.",
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    var floorDropLineWidth: Int = 3

    @Expose
    @ConfigOption(
        name = "Floor Drop Islands",
        desc = "Select the islands where the Line to Floor Drop feature should work.",
    )
    @ConfigEditorDraggableList
    val floorDropIslands: MutableList<FloorDropIsland> = mutableListOf(
        FloorDropIsland.MOONGLADE_MARSH,
        FloorDropIsland.TORRHUS_CANYON,
        FloorDropIsland.CRITTER_SAFARI,
    )

    enum class FloorDropIsland(val displayName: String) {
        MOONGLADE_MARSH("Moonglade Marsh"),
        TORRHUS_CANYON("Torrhus Canyon"),
        CRITTER_SAFARI("Critter Safari"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Line to Floor Drop Distance",
        desc = "Maximum distance between you and a Floor Drop for the line to be shown.",
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 5f, maxValue = 128f)
    var floorDropMaxDistance: Int = 15

    @Expose
    @ConfigOption(
        name = "Floor Drop Scan Radius",
        desc = "Radius in blocks to scan for Floor Drops around the player. Higher values may impact performance.",
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 20f, maxValue = 64f)
    var floorDropScanRadius: Int = 20
}
