package at.hannibal2.skyhanni.config.features.mining;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FossilExcavatorConfig {

    @Expose
    @ConfigOption(name = "Fossil Excavator Solver", desc = "")
    @Accordion
    public FossilExcavatorSolverConfig solver = new FossilExcavatorSolverConfig();

    @Expose
    @ConfigOption(name = "Excavator Profit Tracker", desc = "")
    @Accordion
    public ExcavatorProfitTrackerConfig profitTracker = new ExcavatorProfitTrackerConfig();

}
