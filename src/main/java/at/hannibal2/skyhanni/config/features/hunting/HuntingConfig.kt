package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

/**
 * Attention developers:
 * If your feature can only be used on the foraging islands please mark it with @[OnlyModern]
 */
class HuntingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "To see all Hunting features, please launch the game on a modern version of Minecraft with SkyHanni installed.\n" +
            "§eJoin the SkyHanni Discord for a guide on how to migrate the config.",
    )
    @OnlyLegacy
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Shard Tracker", desc = "")
    @Accordion
    val shardTracker: ShardTrackerConfig = ShardTrackerConfig()

    @Expose
    @ConfigOption(name = "Hunting Profit Tracker", desc = "")
    @Accordion
    val huntingProfitTracker: HuntingProfitTrackerConfig = HuntingProfitTrackerConfig()

    @Expose
    @OnlyModern
    @Category(name = "Galatea Mob Highlights", desc = "Settings for Galatea mob highlights")
    var mobHighlight = GalateaMobHighlightConfig()

    @Expose
    @ConfigOption(name = "Lasso Display", desc = "Displays your lasso progress on screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lassoDisplay = true

    @Expose
    @ConfigLink(owner = HuntingConfig::class, field = "lassoDisplay")
    val lassoDisplayPosition: Position = Position(380, 210)

    @Expose
    @OnlyModern
    @Category(name = "Fusion Keybinds", desc = "Settings for fusion keybinds")
    @SearchTag("hunting box")
    var fusionKeybinds = FusionKeybindsConfig()

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
        name = "Hideonleaf Finder",
        desc = "Shows a route from your position to the nearest possibly spawn point for Hideonleaf for easy finding."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideonleafFinder = false

    @Expose
    @OnlyModern
    @ConfigOption(name = "Show next Hideonleaf", desc = "Press this key to show the next Hideonleaf.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var nextHideonleafKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Fusion Display", desc = "Displays the shard you are fusing and how many you have.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var fusionDisplay = true

    @Expose
    @ConfigLink(owner = HuntingConfig::class, field = "fusionDisplay")
    val fusionDisplayPosition: Position = Position(30, 210)

}
