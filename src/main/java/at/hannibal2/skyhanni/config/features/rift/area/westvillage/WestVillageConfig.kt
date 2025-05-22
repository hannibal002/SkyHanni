package at.hannibal2.skyhanni.config.features.rift.area.westvillage

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WestVillageConfig {
    @ConfigOption(name = "Vermin Tracker", desc = "Track all vermins collected.")
    @Accordion
    @Expose
    var verminTracker: VerminTrackerConfig = VerminTrackerConfig()

    @ConfigOption(name = "Vermin Highlighter", desc = "Highlight vermins.")
    @Accordion
    @Expose
    var verminHighlight: VerminHighlightConfig = VerminHighlightConfig()

    @ConfigOption(name = "Gunther's Race", desc = "")
    @Accordion
    @Expose
    var gunthersRace: GunthersRaceConfig = GunthersRaceConfig()

    @ConfigOption(name = "Kloon Hacking", desc = "")
    @Accordion
    @Expose
    var hacking: KloonHackingConfig = KloonHackingConfig()
}
