package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
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
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lOld Dragon§r§d§l has spawned!§r
     */
    private val chatSpawnPattern by group.pattern(
        "chat.spawn",
        "§5☬ §r§d§lThe §r§5§c§l(?<type>.*)§r§d§l has spawned!§r",
    )

    /**
     * REGEX-TEST: §r§f                           §r§6§lOLD DRAGON DOWN!§r
     */
    private val chatDeath by group.pattern(
        "chat.death",
        "§r§f {27}§r§6§l(?<type>.*) DOWN!§r",
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

    private val nestAreaPattern by group.pattern("area.nest", "Dragon's Nest")

    fun inNestArea() = IslandType.THE_END.isCurrent() && nestAreaPattern.matches(SkyBlockUtils.graphArea)

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
