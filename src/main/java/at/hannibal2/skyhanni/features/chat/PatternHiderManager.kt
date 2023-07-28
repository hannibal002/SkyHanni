package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils

class PatternHiderManager(skyHanniMod: SkyHanniMod) {
    private val watchdogHider = PatternHider("watchdog", watchdogPattern) {
        LorenzUtils.onHypixel && SkyHanniMod.feature.chat.watchDog
    }

    init {
        skyHanniMod.loadModule(watchdogHider)
    }

    companion object {
        val watchdogPattern = listOf(
            Regex("§f"),
            Regex("§4\\[WATCHDOG ANNOUNCEMENT]"),
            Regex("§fWatchdog has banned §r§c§l(.*)§r§f players in the last 7 days."),
            Regex("§fStaff have banned an additional §r§c§l(.*)§r§f in the last 7 days."),
            Regex("§cBlacklisted modifications are a bannable offense!"),
            Regex("§c")
        )
    }
}