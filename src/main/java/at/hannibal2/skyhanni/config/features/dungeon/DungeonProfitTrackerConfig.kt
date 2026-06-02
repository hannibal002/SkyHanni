package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DungeonProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count chest rewards you gain at the end of a dungeon run.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = DungeonProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(
        name = "Show Always",
        desc = "Always show the profit tracker while in a Dungeon or in the Dungeon Hub.",
    )
    @ConfigEditorBoolean
    var showAlways: Boolean = false

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()
}
