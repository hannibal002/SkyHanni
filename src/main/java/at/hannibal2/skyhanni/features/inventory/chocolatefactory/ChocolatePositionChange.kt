package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format

object ChocolatePositionChange {

    private val config get() = ChocolateFactoryAPI.config

    private var lastTime: SimpleTimeMark? = null
    private var lastPosition = -1
    private var lastLeaderboard: String? = null

    fun update(position: Int?, leaderboard: String) {
        if (lastLeaderboard == leaderboard) return

        lastLeaderboard?.let { lastLb ->
            var message = "$lastLb §c-> $leaderboard"
            position?.let { pos ->
                val change = lastPosition - pos
                println()
                val color = if (change > 0) "§a+" else "§c"
                message += "\n §7Changed by $color${change.addSeparators()} spots"
            }
            lastTime?.let {
                message += " §7in §b${it.passedSince().format(maxUnits = 2)}"
            }
            if (config.leaderboardChange) {
                ChatUtils.chat(" \n §6Chocolate Leaderboard Change: §7(SkyHanni)\n $message\n ", prefix = false)
            }
        }

        lastTime = SimpleTimeMark.now()
        lastLeaderboard = leaderboard
        position?.let {
            lastPosition = it
        }
    }
}
