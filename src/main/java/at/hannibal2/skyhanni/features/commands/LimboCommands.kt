package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

object LimboCommands {
    private val config = SkyHanniMod.feature.misc
    var inLimbo = false
    private var joinTime = SimpleTimeMark.farPast()

    fun printPB() {
        val limboPB = config.limboTimePB.seconds
        val userLuck = config.limboTimePB * 0.000810185
        LorenzUtils.chat("§fYour current limbo PB is §e$limboPB§f, granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
    }
    fun printPlaytime(isLimbo: Boolean = false) {
        val playtime = config.limboPlaytime + joinTime.passedSince().inWholeSeconds.toInt()
        if (isLimbo) {
            LorenzUtils.chat("§aYou have ${playtime/3600} hours and ${playtime%3600/60} minutes playtime!",false)
        }
        else LorenzUtils.chat("§fYou have §e${playtime.seconds} §flimbo playtime!")
    }

    fun enterLimbo(limboJoinTime: SimpleTimeMark) {
        joinTime = limboJoinTime
        inLimbo = true
    }
}
