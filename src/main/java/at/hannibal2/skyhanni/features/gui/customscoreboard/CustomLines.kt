package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getPurse
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraft.client.Minecraft

object CustomLines {

    private val replacements = listOf(
        Triple("%x%", { Minecraft.getMinecraft().thePlayer.posX.round(2) }, "X-Coordinate"),
        Triple("%y%", { Minecraft.getMinecraft().thePlayer.posY.round(2) }, "Y-Coordinate"),
        Triple("%z%", { Minecraft.getMinecraft().thePlayer.posZ.round(2) }, "Z-Coordinate"),
        Triple("%dir%", { Minecraft.getMinecraft().thePlayer.rotationYaw.round(2) }, "Direction"),
        Triple("%purse%", { getPurse() }, "Purse"),
        Triple("%bits%", { getBits() }, "Bits"),
        Triple("%bits_available%", { getBitsAvailable() }, "Bits Available"),
        Triple("%island%", { HypixelData.skyBlockIsland.displayName }, "Island"),
        Triple("%area%", { HypixelData.skyBlockArea }, "Area"),
    )

    internal fun String.handleCustomLine(): List<String> {
        return this.replace("&", "§").replaceWithReplacements()
    }

    private fun String.replaceWithReplacements(): List<String> {
        var modifiedString = this
        replacements.forEach { (placeholder, replacement) ->
            modifiedString = modifiedString.replace(placeholder, replacement.invoke().toString())
        }
        return modifiedString.split("\\n")
    }
}
