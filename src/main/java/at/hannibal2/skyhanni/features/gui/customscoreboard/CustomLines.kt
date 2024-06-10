package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getPurse
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import net.minecraft.client.Minecraft

object CustomLines {

    internal val replacements = listOf(
        Triple("%x%", { Minecraft.getMinecraft().thePlayer.posX.round(2) }, "X-Coordinate"),
        Triple("%y%", { Minecraft.getMinecraft().thePlayer.posY.round(2) }, "Y-Coordinate"),
        Triple("%z%", { Minecraft.getMinecraft().thePlayer.posZ.round(2) }, "Z-Coordinate"),
        Triple("%yaw%", { normalizeYaw(Minecraft.getMinecraft().thePlayer.rotationYaw).round(2) }, "Direction"),
        Triple("%pitch%", { Minecraft.getMinecraft().thePlayer.rotationPitch.round(2) }, "Pitch"),
        Triple("%purse%", { getPurse() }, "Purse"),
        Triple("%bits%", { getBits() }, "Bits"),
        Triple("%bits_available%", { getBitsAvailable() }, "Bits Available"),
        Triple("%island%", { HypixelData.skyBlockIsland.displayName }, "Island"),
        Triple("%area%", { HypixelData.skyBlockArea }, "Area"),
        Triple("%date%", { SkyBlockTime.now().formatted(hoursAndMinutesElement = false, yearElement = false) }, "Date"),
        Triple(
            "%year%",
            { SkyBlockTime.now().formatted(dayAndMonthElement = false, hoursAndMinutesElement = false) },
            "Year",
        ),
        Triple("%time%", { SkyBlockTime.now().formatted(dayAndMonthElement = false, yearElement = false) }, "Time"),
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

    private fun normalizeYaw(yaw: Float): Float {
        var result = yaw % 360
        if (result > 180) {
            result -= 360
        } else if (result <= -180) {
            result += 360
        }
        return result
    }


}
