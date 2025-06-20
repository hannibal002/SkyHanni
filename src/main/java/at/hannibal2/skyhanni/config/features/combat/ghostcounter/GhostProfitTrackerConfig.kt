package at.hannibal2.skyhanni.config.features.combat.ghostcounter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.ghosttracker.GhostTracker.GhostTrackerLines
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GhostProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables the Ghost Profit Tracker.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Display Text", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    var ghostTrackerText: MutableList<GhostTrackerLines> = mutableListOf(
        GhostTrackerLines.KILLS,
        GhostTrackerLines.GHOSTS_SINCE_SORROW,
        GhostTrackerLines.MAX_KILL_COMBO,
        GhostTrackerLines.COMBAT_XP_GAINED,
        GhostTrackerLines.AVERAGE_MAGIC_FIND,
        GhostTrackerLines.BESTIARY_KILLS
    )

    @ConfigOption(
        name = "Max Bestiary",
        desc = "§7This feature will currently not work properly when having max Ghost Bestiary."
    )
    @ConfigEditorInfoText(infoTitle = "Warning")
    var useless: String? = null

    @Expose
    @ConfigLink(owner = GhostProfitTrackerConfig::class, field = "enabled")
    var position: Position = Position(50, 50)
}
