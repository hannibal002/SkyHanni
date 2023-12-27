package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object LimboCommands {
    private val config = SkyHanniMod.feature.misc
    var inLimbo = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            inLimbo = true
        }
    }

    fun printPB() {
        val limboPB = config.limboTimePB.seconds
        val userLuck = config.limboTimePB * 0.000810185
        LorenzUtils.chat("§fYour current limbo PB is §e$limboPB§f, granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
    }
    fun printPlaytime() {
        LorenzUtils.chat("your playtime is ${config.limboPlaytime.seconds}!!! so wow")
    }
}
