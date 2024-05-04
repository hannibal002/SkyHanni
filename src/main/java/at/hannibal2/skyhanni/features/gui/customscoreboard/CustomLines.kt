package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getPurse

object CustomLines {

    private val replacements = mapOf(
        "%purse%" to { getPurse() },
        "%bits%" to { getBits() },
        "%bitsAvailable%" to { getBitsAvailable() }
    )

    internal fun String.handleCustomLine(): List<String> {
        return this.replace("&", "§").replaceWithReplacements()
    }

    private fun String.replaceWithReplacements(): List<String> {
        var modifiedString = this
        replacements.forEach { (placeholder, replacement) ->
            modifiedString = modifiedString.replace(placeholder, replacement.invoke())
        }
        return modifiedString.split("\\n")
    }
}
