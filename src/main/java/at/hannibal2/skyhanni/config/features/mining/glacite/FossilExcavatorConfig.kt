package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class FossilExcavatorConfig {
    @Expose
    @ConfigOption(name = "Fossil Excavator Solver", desc = "")
    @Accordion
    val solver: FossilExcavatorSolverConfig = FossilExcavatorSolverConfig()

    @Expose
    @ConfigOption(name = "Excavator Profit Tracker", desc = "")
    @Accordion
    val profitTracker: ExcavatorProfitTrackerConfig = ExcavatorProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Excavator Tooltip Hider", desc = "")
    @Accordion
    val tooltipHider: ExcavatorTooltipHiderConfig = ExcavatorTooltipHiderConfig()

    @Expose
    @ConfigOption(name = "Get Scrap from Sacks", desc = "")
    @Accordion
    val scrapGFS: ExcavatorScrapGFSConfig = ExcavatorScrapGFSConfig()

    @Expose
    @ConfigOption(
        name = "Profit per Excavation",
        desc = "Show profit/loss in chat after each excavation. Also includes breakdown information on hover."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var profitPerExcavation: Boolean = false

    @Expose
    @ConfigOption(name = "Ironman Profits", desc = "Removes the cost of Scrap from Profit.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ironmanProfitCalc: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Glacite Powder Stack", desc = "Show Glacite Powder as stack size in the Fossil Excavator.")
    @ConfigEditorBoolean
    @FeatureToggle
    var glacitePowderStack: Boolean = false
}
