package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

object LimboCommands {
    private val storage get() = ProfileStorageData.playerSpecific?.limbo
    private var joinTime = SimpleTimeMark.farPast()
    var inLimbo = false

    fun printPB() {
        val limboPB = storage?.personalBest?.seconds
        val userLuck = storage?.userLuck ?: 0f
        ChatUtils.chat("§fYour current limbo PB is §e$limboPB§f, granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
    }
    fun printPlaytime(isLimbo: Boolean = false) {
        val storedPlaytime = storage?.playtime ?: 0
        val playtime = storedPlaytime + joinTime.passedSince().inWholeSeconds.toInt()
        if (isLimbo) {
            ChatUtils.chat("§aYou have ${playtime/3600} hours and ${playtime%3600/60} minutes playtime!",false)
        }
        else ChatUtils.chat("§fYou have §e${playtime.seconds} §flimbo playtime!")
    }

    fun enterLimbo(limboJoinTime: SimpleTimeMark) {
        joinTime = limboJoinTime
        inLimbo = true
    }
}
