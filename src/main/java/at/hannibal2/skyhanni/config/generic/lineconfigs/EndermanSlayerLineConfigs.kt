package at.hannibal2.skyhanni.config.generic.lineconfigs

import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.utils.LorenzColor
import io.github.notenoughupdates.moulconfig.ChromaColour

class EndermanSlayerLineConfigs {

    class LineToBeacon : LineToConfig(defaultOn = true, defaultColor = ChromaColour.fromStaticRGB(255, 0, 88, 255))
    class LineToNukekebi : LineToConfig(defaultColor = LorenzColor.GOLD.toChromaColor())
}
