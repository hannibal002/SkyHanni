package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object DragonFightAPI {

    var currentType: String? = null
    var currentHp: Int? = null
    private var yourDamage: Int? = null

    private val group = RepoPattern.group("combat.end-dragon-fight")

    /**
     * REGEX-TEST: ☬ The Old Dragon has spawned!
     */
    private val chatSpawnPattern by group.pattern(
        "chat.spawn",
        "☬ The (?<type>.+) has spawned!",
    )

    /**
     * WRAPPED-REGEX-TEST: "                           OLD DRAGON DOWN!"
     * WRAPPED-REGEX-FAIL: "                    END STONE PROTECTOR DOWN!"
     */
    private val chatDeath by group.pattern(
        "chat.death",
        " +\\w+ DRAGON DOWN!",
    )

    /**
     * REGEX-TEST: Dragon HP: 4,824,217 ❤
     */
    private val scoreboardHPPattern by group.pattern(
        "scoreboard.hp",
        "Dragon HP: (?<hp>.+) ❤",
    )

    /**
     * REGEX-TEST: Your Damage: 0
     */
    private val scoreboardYourDamagePattern by group.pattern(
        "scoreboard.your-damage",
        "Your Damage: (?<damage>[\\d.,]+)",
    )

    private val nestAreaPattern by group.pattern("area.nest", "Dragon's Nest")

    fun inNestArea() = IslandType.THE_END.isInIsland() && nestAreaPattern.matches(SkyBlockUtils.graphArea)

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        chatSpawnPattern.matchMatcher(event.cleanMessage) {
            currentType = group("type")
            return
        }
        chatDeath.matchMatcher(event.cleanMessage) {
            reset()
            return
        }
    }

    fun reset() {
        currentType = null
        currentHp = null
        yourDamage = null
    }

    @HandleEvent
    fun onWorldChange() {
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
