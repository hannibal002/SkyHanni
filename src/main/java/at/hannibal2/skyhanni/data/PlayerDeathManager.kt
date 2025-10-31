package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.player.PlayerDeathEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object PlayerDeathManager {

    /**
     * REGEX-TEST: §c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
     */
    private val deathMessagePattern by RepoPattern.pattern(
        "chat.player.death",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        val message = event.message
        deathMessagePattern.matchMatcher(message) {
            val name = group("name")
            val reason = group("reason").removeColor()
            PlayerDeathEvent(name, reason, event).post()
        }
    }
}
