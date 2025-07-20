package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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
}
