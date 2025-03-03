package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.features.misc.DraconicSacrificeTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EndIslandConfig {
    @Expose
    @ConfigOption(name = "Draconic Sacrifice Tracker", desc = "")
    @Accordion
    var draconicSacrificeTracker: DraconicSacrificeTrackerConfig = DraconicSacrificeTrackerConfig()

    @Expose
    @ConfigOption(name = "Dragon Profit Tracker", desc = "")
    @Accordion
    var dragonProfitTracker: DragonProfitTrackerConfig = DragonProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Dragon Features", desc = "")
    @Accordion
    var dragon: DragonConfig = DragonConfig()
}
