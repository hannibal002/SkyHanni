package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Area
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Bits
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.BitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Date
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Island
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Pitch
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Purse
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Time
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.XCoord
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.YCoord
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Yaw
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.Year
import at.hannibal2.skyhanni.features.gui.customscoreboard.replacements.ZCoord

object CustomLines {

    internal val replacements = listOf(
        XCoord,
        YCoord,
        ZCoord,
        Yaw,
        Pitch,
        Purse,
        Bits,
        BitsAvailable,
        Island,
        Area,
        Date,
        Year,
        Time,
    )

    internal fun String.handleCustomLine(): List<String> {
        return this.replace("&&", "§").replaceWithReplacements()
    }

    private fun String.replaceWithReplacements(): List<String> {
        var modifiedString = this
        replacements.forEach {
            modifiedString = modifiedString.replace(it.trigger, it.replacement())
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
