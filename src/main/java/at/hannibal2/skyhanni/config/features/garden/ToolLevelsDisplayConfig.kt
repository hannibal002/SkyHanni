package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class ToolLevelsDisplayConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Displays an overlay for farming tools leveling progress."
    )
    @ConfigEditorBoolean
    @SearchTag("hoe, axe")
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Overflow",
        desc = "Displays overflow levels for level 50 tools."
    )
    @ConfigEditorBoolean
    var overflow: Boolean = true

    @Expose
    @ConfigOption(
        name = "Mute Hoe Sounds",
        desc = "Mutes the sound that plays when you level up the tool."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var muteHoeSounds: Boolean = true

    @Expose
    @ConfigLink(owner = ToolLevelsDisplayConfig::class, field = "enabled")
    val position: Position = Position(100, 100, true)
}
