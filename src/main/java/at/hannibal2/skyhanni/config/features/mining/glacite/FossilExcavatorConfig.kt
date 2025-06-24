package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FossilExcavatorConfig {
    @Expose
    @ConfigOption(name = "Fossil Excavator Solver", desc = "")
    @Accordion
    var solver: FossilExcavatorSolverConfig = FossilExcavatorSolverConfig()

    @Expose
    @ConfigOption(name = "Excavator Profit Tracker", desc = "")
    @Accordion
    var profitTracker: ExcavatorProfitTrackerConfig = ExcavatorProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Excavator Tooltip Hider", desc = "")
    @Accordion
    var tooltipHider: ExcavatorTooltipHiderConfig = ExcavatorTooltipHiderConfig()

    @Expose
    @ConfigOption(
        name = "Profit per Excavation",
        desc = "Show profit/loss in chat after each excavation. Also includes breakdown information on hover."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var profitPerExcavation: Boolean = false

    @Expose
    @ConfigOption(name = "Glacite Powder Stack", desc = "Show Glacite Powder as stack size in the Fossil Excavator.")
    @ConfigEditorBoolean
    @FeatureToggle
    var glacitePowderStack: Boolean = false
}
