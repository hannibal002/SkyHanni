package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TreeProgressConfig {

    @Expose
    @ConfigOption(name = "Tree Progress Display", desc = "Displays your tree progress on screen.")
    @ConfigEditorBoolean
    @OnlyModern
    @FeatureToggle
    var enabled = true

    @Expose
    @ConfigLink(owner = TreeProgressConfig::class, field = "enabled")
    val position: Position = Position(30, -130)

    @Expose
    @ConfigOption(name = "Only Holding Axe", desc = "Only show the tracker while holding an axe.")
    @ConfigEditorBoolean
    @OnlyModern
    var onlyHoldingAxe: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Display", desc = "Shows a compact version of the display.")
    @ConfigEditorBoolean
    @OnlyModern
    var compact: Boolean = false

}
