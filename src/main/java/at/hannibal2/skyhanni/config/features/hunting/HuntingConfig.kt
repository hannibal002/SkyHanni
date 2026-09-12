package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.foraging.SafariConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
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
    @ConfigOption(name = "Reel Alert", desc = "Plays a sound alert when it's time to reel in your lasso.")
    @ConfigEditorBoolean
    @FeatureToggle
    var reelAlert = true

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

}
