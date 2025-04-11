package at.hannibal2.skyhanni.config.features.rift.area.livingcave

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LivingCaveConfig {
    @Expose
    @ConfigOption(name = "Living Metal Suit Progress", desc = "")
    @Accordion
    var livingMetalSuitProgress: LivingMetalSuitProgressConfig = LivingMetalSuitProgressConfig()

    @Expose
    @ConfigOption(name = "Defense Blocks", desc = "")
    @Accordion
    var defenseBlock: DefenseBlockConfig = DefenseBlockConfig()

    @Expose
    @ConfigOption(name = "Living Metal Helper", desc = "")
    @Accordion
    var livingMetal: LivingCaveLivingMetalConfig = LivingCaveLivingMetalConfig()

    @Expose
    @ConfigOption(name = "Living Metal Snake Helper", desc = "")
    @Accordion
    var snakeHelper: LivingSnakeHelperConfig = LivingSnakeHelperConfig()
}
