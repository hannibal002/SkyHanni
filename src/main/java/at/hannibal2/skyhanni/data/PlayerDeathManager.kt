package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object PlayerDeathManager {

    /**
     * WRAPPED-REGEX-TEST: " ☠ ZeroHazel was killed by Ashfang."
     * WRAPPED-REGEX-TEST: " ☠ You fell into the void."
     * WRAPPED-REGEX-TEST: " ☠ You burned to death."
     * WRAPPED-REGEX-TEST: " ☠ You were killed by Bladesoul."
     */
    private val deathMessagePattern by RepoPattern.pattern(
        "chat.player.death-nocolor",
        " ☠ (?<name>\\w+) (?<reason>.+)",
    )

    private fun handleDeath(message: String): Pair<String, String>? =
        deathMessagePattern.matchMatcher(message) {
            val name = group("name").takeUnless { it == "You" } ?: PlayerUtils.getName()
            val reason = group("reason")
            name to reason
        }

    @HandleEvent
    fun onAllowChat(event: SkyHanniChatEvent.Allow) {
        val (name, reason) = handleDeath(event.cleanMessage) ?: return
        PlayerDeathEvent.Allow(name, reason, event).post()
    }

    @HandleEvent
    fun onModifyChat(event: SkyHanniChatEvent.Modify) {
        val (name, reason) = handleDeath(event.cleanMessage) ?: return
        PlayerDeathEvent.Modify(name, reason, event).post()
    }
}
