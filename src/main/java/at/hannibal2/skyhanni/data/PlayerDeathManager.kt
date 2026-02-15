package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object PlayerDeathManager {

    /**
     * REGEX-TEST: §c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
     */
    private val deathMessagePattern by RepoPattern.pattern(
        "chat.player.death",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)",
    )

    /**
     * REGEX-TEST:  ☠ You fell into the void.
     * REGEX-TEST:  ☠ You burned to death.
     * REGEX-TEST:  ☠ You were killed by Bladesoul.
     */
    private val selfDeathMessagePattern by RepoPattern.pattern(
        "chat.player.selfdeath",
        " ☠ You (?<reason>.+)",
    )


    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        deathMessagePattern.matchMatcher(event.message) {
            val name = group("name")
            val reason = group("reason").removeColor()
            PlayerDeathEvent(name, reason, event).post()
        }

        selfDeathMessagePattern.matchMatcher(event.cleanMessage) {
            val name = PlayerUtils.getName()
            val reason = group("reason")
            PlayerDeathEvent(name, reason, event).post()
        }
    }
}
