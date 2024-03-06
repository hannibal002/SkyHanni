package at.hannibal2.skyhanni.config.features.garden.pests;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PestsConfig {

    @Expose
    @ConfigOption(name = "Pest Spawn", desc = "")
    @Accordion
    public PestSpawnConfig pestSpawn = new PestSpawnConfig();

    @Expose
    @ConfigOption(name = "Pest Finder", desc = "")
    @Accordion
    public PestFinderConfig pestFinder = new PestFinderConfig();

    @Expose
    @ConfigOption(name = "Pest Timer", desc = "")
    @Accordion
    public PestTimerConfig pestTimer = new PestTimerConfig();

    @Expose
    @ConfigOption(name = "Spray", desc = "")
    @Accordion
    public SprayConfig spray = new SprayConfig();
}
