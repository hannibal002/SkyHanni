package at.hannibal2.skyhanni.config.features.event.winter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WinterConfig {

    @Expose
    @ConfigOption(name = "Frozen Treasure Tracker", desc = "")
    @Accordion
    val frozenTreasureTracker: FrozenTreasureConfig = FrozenTreasureConfig()

    @Expose
    @ConfigOption(name = "Frozen Treasure Highlighter", desc = "")
    @Accordion
    val frozenTreasureHighlighter: FrozenTreasureHighlighterConfig = FrozenTreasureHighlighterConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Refined Bottle of Jyrre Timer", desc = "")
    val jyrreTimer: JyrreTimerConfig = JyrreTimerConfig()

    @Expose
    @ConfigOption(
        name = "Island Close Time",
        desc = "While on the Winter Island, show a timer until Jerry's Workshop closes.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var islandCloseTime: Boolean = true

    @Expose
    @ConfigLink(owner = WinterConfig::class, field = "islandCloseTime")
    val islandCloseTimePosition: Position = Position(10, 10)

    @Expose
    @ConfigOption(
        name = "New Year Cake Reminder",
        desc = "Send a reminder while the New Year Cake can be collected in the hub.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var newYearCakeReminder: Boolean = true

    @Expose
    @ConfigOption(
        name = "Reindrake Warp Helper",
        desc = "Sends a clickable message in chat to warp to the Winter Island spawn when a Reindrake spawns.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var reindrakeWarpHelper: Boolean = true
}
