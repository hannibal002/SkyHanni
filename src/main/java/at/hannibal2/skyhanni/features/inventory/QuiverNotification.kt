package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object QuiverNotification {
    private val quiverChatPattern by RepoPattern.pattern(
        "inventory.quiver.chat.low",
        "§cYou only have (?<arrowsLeft>.*) arrows left in your Quiver!"
    )
    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!SkyHanniMod.configManager.features.inventory.quiverAlert) return
        quiverChatPattern.matchMatcher(event.message) {
            TitleManager.sendTitle("§c${group("arrowsLeft")} arrows left!", 3.seconds, 3.6, 7f)
            SoundUtils.repeatSound(100, 30, SoundUtils.plingSound)
        }
    }
}
