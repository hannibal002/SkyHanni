package at.hannibal2.skyhanni.config.features.rift.area.westvillage;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class WestVillageConfig {

    @ConfigOption(name = "Kloon Hacking", desc = "")
    @Accordion
    @Expose
    public KloonHackingConfig hacking = new KloonHackingConfig();

    @ConfigOption(name = "Vermin Tracker", desc = "Track all vermins collected.")
    @Accordion
    @Expose
    public VerminTrackerConfig verminTracker = new VerminTrackerConfig();

    @ConfigOption(name = "Vermin Highlighter", desc = "Highlight vermins.")
    @Accordion
    @Expose
    public VerminHighlightConfig verminHighlight = new VerminHighlightConfig();
}
