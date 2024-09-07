package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PlayerDeathManager {

    /**
     * REGEX-TEST: §c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
     */
    private val deathMessagePattern by RepoPattern.pattern(
        "chat.player.death",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)",
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message
        deathMessagePattern.matchMatcher(message) {
            val name = group("name")
            val reason = group("reason").removeColor()
            PlayerDeathEvent(name, reason, event).postAndCatch()
        }
    }
}
