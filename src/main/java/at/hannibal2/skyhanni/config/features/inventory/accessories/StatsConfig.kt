package at.hannibal2.skyhanni.config.features.inventory.accessories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StatsConfig {

    @Expose
    @ConfigOption(name = "Magical Power Display", desc = "")
    @Accordion
    var magicalPower: MagicalPowerConfig = MagicalPowerConfig()

    @Expose
    @ConfigOption(name = "Stats Tuning", desc = "")
    @Accordion
    var statsTuning: StatsTuningConfig = StatsTuningConfig()

    @Expose
    @ConfigOption(name = "Acessory Display", desc = "")
    @Accordion
    var overviewDisplay: AccessoryOverviewDisplayConfig = AccessoryOverviewDisplayConfig()
}
