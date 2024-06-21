package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object ServerClose : Event() {
    override fun getDisplay() = buildList {
        ServerRestartTitle.restartingGreedyPattern.firstMatches(getSbLines())?.let {
            add(it.split("§8")[0])
        }
    }

    override fun showWhen() = true

    override val configLine = "§cServer closing soon!"
}
