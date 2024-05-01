package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format

object ChocolatePositionChange {

    private val config get() = ChocolateFactoryAPI.config
    private val storage get() = ChocolateFactoryAPI.profileStorage?.positionChange

    fun update(position: Int?, leaderboard: String) {
        position ?: return
        val storage = storage ?: return
        val lastTime = storage.lastTime?.let { SimpleTimeMark(it) }
        val lastPosition = storage.lastPosition
        val lastLeaderboard = storage.lastLeaderboard

        if (lastLeaderboard == leaderboard) return

        lastLeaderboard?.let { lastLb ->
            var message = "§b$lastLb §c-> §b$leaderboard"
            val change = lastPosition - position
            val color = if (change > 0) "§a+" else "§c"
            message += "\n §7Changed by $color${change.addSeparators()} spots"

            lastTime?.let {
                message += " §7in §b${it.passedSince().format(maxUnits = 2)}"
            }
            if (config.leaderboardChange && lastPosition != -1) {
                ChatUtils.chat(" \n §6Chocolate Leaderboard Change: §7(SkyHanni)\n $message\n ", prefix = false)
            }
        }

        storage.lastTime = SimpleTimeMark.now().toMillis()
        storage.lastLeaderboard = leaderboard
        storage.lastPosition = position
    }
}
