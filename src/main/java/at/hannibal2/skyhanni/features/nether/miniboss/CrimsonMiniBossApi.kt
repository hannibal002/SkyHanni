package at.hannibal2.skyhanni.features.nether.miniboss

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.combat.CrimsonMinibossEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CrimsonMiniBossApi {
    private val patternGroup = RepoPattern.group("crimson.miniboss")

    /**
     * REGEX-TEST: BEWARE - Bladesoul Is Spawning.
     */
    private val spawnPattern by patternGroup.pattern(
        "spawn",
        "BEWARE - (?<name>${getRegexNames()}) Is Spawning\\.",
    )

    /**
     * WRAPPED-REGEX-TEST: "                            BLADESOUL DOWN!"
     */
    private val downPattern by patternGroup.pattern(
        "down",
        " *(?<name>${getRegexUppercaseNames()}) DOWN!",
    )

    private fun getRegexNames(): String = CrimsonMiniBoss.entries.joinToString("|") { it.displayName }
    private fun getRegexUppercaseNames(): String = CrimsonMiniBoss.entries.joinToString("|") { it.displayName.uppercase() }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        spawnPattern.matchMatcher(event.cleanMessage) {
            val miniboss = CrimsonMiniBoss.fromName(group("name")) ?: return
            CrimsonMinibossEvent.Spawning(miniboss).post()
            return
        }
        downPattern.matchMatcher(event.cleanMessage) {
            val miniboss = CrimsonMiniBoss.fromName(group("name")) ?: return
            CrimsonMinibossEvent.Death(miniboss).post()
            return
        }
    }
}
