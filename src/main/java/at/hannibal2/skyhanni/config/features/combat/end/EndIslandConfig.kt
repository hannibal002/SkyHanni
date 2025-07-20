package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
    @ConfigOption(name = "Weight Endstone Protector", desc = "Shows your Endstone Protector weight in chat after it died.")
    @ConfigEditorBoolean
    @FeatureToggle
    var endstoneProtectorChat: Boolean = true

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    val enderNodeTracker: EnderNodeConfig = EnderNodeConfig()
}
