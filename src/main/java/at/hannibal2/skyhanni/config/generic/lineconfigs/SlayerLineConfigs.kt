package at.hannibal2.skyhanni.config.generic.lineconfigs

import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.utils.LorenzColor

class SlayerLineConfigs {

    class SlayerLineDefaultOn : LineToConfig(defaultColor = LorenzColor.AQUA.toChromaColor(), defaultOn = true)
    class SlayerLineDefaultOff : LineToConfig(defaultColor = LorenzColor.AQUA.toChromaColor())

}
