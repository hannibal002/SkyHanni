package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.hunting.ShardTrackerDisplay
import at.hannibal2.skyhanni.utils.OSUtils.openBrowser
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class ShardTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track your current shards.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = ShardTrackerConfig::class, field = "enabled")
    val position: Position = Position(80, 180)

    @Expose
    @ConfigOption(name = "Select Shard Key", desc = "Press this key in your hunting box to track the hovered shard.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var selectShardKeybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @ConfigOption(
        name = "SkyShards",
        desc = "Click this button to import a recipe from SkyShards.\nYou can also use §e/shimportskyshards§f."
    )
    @ConfigEditorButton(buttonText = "Import")
    val skyShardsImport: Runnable = Runnable { ShardTrackerDisplay.importFromSkyShards() }

    @ConfigOption(name = "SkyShards", desc = "Click this button to open SkyShards in the browser.")
    @ConfigEditorButton(buttonText = "Open")
    val openSkyShards: Runnable = Runnable { openBrowser("https://skyshards.com/") }
}
