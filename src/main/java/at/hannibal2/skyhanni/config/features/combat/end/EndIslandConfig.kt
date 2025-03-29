package at.hannibal2.skyhanni.config.features.combat.end

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EndIslandConfig {
    @Expose
    @ConfigOption(name = "Draconic Sacrifice Tracker", desc = "")
    @Accordion
    var draconicSacrificeTracker: DraconicSacrificeTrackerConfig = DraconicSacrificeTrackerConfig()

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    var enderNodeTracker: EnderNodeConfig = EnderNodeConfig()
}
