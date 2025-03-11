package at.hannibal2.skyhanni.config.features.inventory.accessories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AccessoryConfig {

    @Expose
    @ConfigOption(name = "Magical Power Display", desc = "")
    @Accordion
    var magicalPower: MagicalPowerConfig = MagicalPowerConfig()

    @Expose
    @ConfigOption(name = "Stats Tuning", desc = "")
    @Accordion
    var statsTuning: StatsTuningConfig = StatsTuningConfig()

    @Expose
    @ConfigOption(name = "Overview Display", desc = "")
    @Accordion
    var overviewDisplay: AccessoryOverviewDisplayConfig = AccessoryOverviewDisplayConfig()
}
