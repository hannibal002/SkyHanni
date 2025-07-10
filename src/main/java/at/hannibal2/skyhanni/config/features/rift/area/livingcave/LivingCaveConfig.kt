package at.hannibal2.skyhanni.config.features.rift.area.livingcave

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LivingCaveConfig {
    @Expose
    @ConfigOption(name = "Living Metal Suit Progress", desc = "")
    @Accordion
    val livingMetalSuitProgress: LivingMetalSuitProgressConfig = LivingMetalSuitProgressConfig()

    @Expose
    @ConfigOption(name = "Defense Blocks", desc = "")
    @Accordion
    val defenseBlock: DefenseBlockConfig = DefenseBlockConfig()

    @Expose
    @ConfigOption(name = "Living Metal Helper", desc = "")
    @Accordion
    val livingMetal: LivingCaveLivingMetalConfig = LivingCaveLivingMetalConfig()

    @Expose
    @ConfigOption(name = "Living Metal Snake Helper", desc = "")
    @Accordion
    val snakeHelper: LivingSnakeHelperConfig = LivingSnakeHelperConfig()
}
