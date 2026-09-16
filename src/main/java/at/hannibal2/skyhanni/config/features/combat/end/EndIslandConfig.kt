package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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

    /**
     * Every move of an End island option, kept together here rather than in whichever feature class
     * happens to read the option today: they describe the config tree, not a feature.
     */
    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(78, "combat.dragon", "combat.endIsland.dragon")
            event.move(78, "combat.endstoneProtectorChat", "combat.endIsland.endstoneProtectorChat")
            // Moved rather than dropped: the option was on by default, and a reset would silently
            // switch it off.
            event.move(147, "combat.endIsland.endstoneProtectorChat", "combat.endIsland.golem.weightChat")
        }
    }
}
