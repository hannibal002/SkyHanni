package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object DragonFightAPI {

    var currentType: String? = null
    var currentHp: Int? = null
    var yourDamage: Int? = null

    private val group = RepoPattern.group("combat.end-dragon-fight")

    /**
     * REGEX-TEST: ☬ The Wise Dragon has spawned!
     */
    private val chatSpawnPattern by group.pattern(
        "chat.spawn",
        "☬ The (?<type>.*) Dragon has spawned!",
    )

    /**
     * REGEX-TEST:                           YOUNG DRAGON DOWN!
     */
    private val chatDeath by group.pattern(
        "chat.death",
        ".*DRAGON DOWN!",
    )

    /**
     * REGEX-TEST: Dragon HP: 4,824,217 ❤
     */
    private val scoreboardHPPattern by group.pattern(
        "scoreboard.hp",
        "Dragon HP: (?<hp>.*) ❤",
    )

    /**
     * REGEX-TEST: Your Damage: 0
     */
    private val scoreboardYourDamagePattern by group.pattern(
        "scoreboard.your-damage",
        "Your Damage: (?<damage>.*)",
    )

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        chatSpawnPattern.matchMatcher(event.message.removeColor()) {
            currentType = group("type")
        }
        chatDeath.matchMatcher(event.message.removeColor()) {
            reset()
        }
    }

    fun reset() {
        currentType = null
        currentHp = null
        yourDamage = null
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        for (line in event.added.map { it.removeColor() }) {
            scoreboardHPPattern.matchMatcher(line) {
                currentHp = group("hp").formatInt()
            }
            scoreboardYourDamagePattern.matchMatcher(line) {
                yourDamage = group("damage").formatInt()
            }
        }
    }
}
