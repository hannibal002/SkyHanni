package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FossilExcavatorSolverConfig {
    @Expose
    @ConfigOption(
        name = "Fossil Excavator Helper",
        desc = "Helper for finding fossils in the fossil excavator.\n" +
            "§eWill always solve if you have at least 18 clicks. Solves everything except Spine, Ugly and Helix in 16 clicks."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Percentage",
        desc = "Shows percentage chance that next click will be a fossil.\n" +
            "§eThis assumes there is a fossil hidden in the dirt."
    )
    @ConfigEditorBoolean
    var showPercentage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Solver Objective",
        desc = "Toggle between locating fossils or avoiding them entirely."
    )
    @ConfigEditorDropdown
    var mode: SolverMode = SolverMode.FOSSIL

    enum class SolverMode(val displayName: String) {
        FOSSIL("Find Fossil"),
        AVOID("Avoid Fossil"),
        ;

        override fun toString(): String {
            return displayName
        }
    }

    @Expose
    @ConfigOption(
        name = "Enforce Guaranteed Hits",
        desc = "Blocks non-optimal clicks when a 100% certain tile is available."
    )
    @ConfigEditorBoolean
    var blockClicks: Boolean = false

    @Expose
    @ConfigLink(owner = FossilExcavatorSolverConfig::class, field = "enabled")
    val position: Position = Position(183, 212)
}
