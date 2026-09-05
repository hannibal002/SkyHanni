package at.hannibal2.skyhanni.config.features.combat.end

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EndIslandConfig {
    @Expose
    @ConfigOption(name = "Draconic Sacrifice Tracker", desc = "")
    @Accordion
    val draconicSacrificeTracker: DraconicSacrificeTrackerConfig = DraconicSacrificeTrackerConfig()

    @Expose
    @ConfigOption(name = "Dragon Features", desc = "")
    @Accordion
    val dragon: DragonConfig = DragonConfig()

    @Expose
    @ConfigOption(name = "Golem Features", desc = "End Stone Protector features.")
    @Accordion
    val golem: GolemConfig = GolemConfig()

    @Expose
    @ConfigOption(name = "Rare Drop Tracker", desc = "")
    @Accordion
    val rareDropTracker: RareDropTrackerConfig = RareDropTrackerConfig()

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    val enderNodeTracker: EnderNodeConfig = EnderNodeConfig()
}
