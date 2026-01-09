package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HoeLevelsDisplayConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Displays an overlay for hoe leveling progress."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Overflow",
        desc = "Displays overflow levels for level 50 hoes."
    )
    @ConfigEditorBoolean
    var overflow: Boolean = true

    @Expose
    @ConfigOption(
        name = "Mute Hoe Sounds",
        desc = "Mutes the sound that plays when you level up the hoe."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var muteHoeSounds: Boolean = true

    @Expose
    @ConfigLink(owner = HoeLevelsDisplayConfig::class, field = "enabled")
    val position: Position = Position(100, 100, true)
}
