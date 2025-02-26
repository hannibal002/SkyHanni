package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object DragonFightAPI {

    var currentType: String? = null
    var currentHp: Int? = null
    private var yourDamage: Int? = null
    var yourEyesPlaced: Int = 0

    private val group = RepoPattern.group("combat.end-dragon-fight")

    /**
     * REGEX-TEST: ☬ You placed a Summoning Eye! (1/8)
     * REGEX-TEST: §r§5☬ §r§dYou placed a Summoning Eye! §r§7(§r§e1§r§7/§r§a8§r§7)§r
     */
    private val chatEyePlacedPattern by group.pattern(
        "chat.eye-placed",
        "§r§5☬ §r§dYou placed a Summoning Eye! §r§7\\(§r§e(?<placed>.*)§r§7/§r§a8§r§7\\)§r",
    )

    /**
     * §r§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7(§r§a8§r§7/§r§a8§r§7)§r
     */
    private val chatEyePlacedFinalPattern by group.pattern(
        "chat.eye-placed-final",
        "§r§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7\\(§r§a8§r§7/§r§a8§r§7\\)§r",
    )

    /**
     * REGEX-TEST: §r§5You recovered a Summoning Eye!§r
     */
    private val chatEyeRecoveredPattern by group.pattern(
        "chat.eye-recovered",
        "§r§5You recovered a Summoning Eye!§r"
    )

    /**
     * REGEX-TEST: §r§5Your Sleeping Eyes have been awoken by the magic of the Dragon. They are now Remnants of the Eye!§r
     */
    private val chatEyeAwokenPattern by group.pattern(
        "chat.eye-awoken",
        "Your Sleeping Eyes have been awoken by the magic of the Dragon. They are now Remnants of the Eye!",
    )

    /**
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lOld Dragon§r§d§l has spawned!§r
     */
    private val chatSpawnPattern by group.pattern(
        "chat.spawn",
        "§5☬ §r§d§lThe §r§5§c§l(?<type>.*)§r§d§l has spawned!§r",
    )

    /**
     * REGEX-TEST:                           YOUNG DRAGON DOWN!
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

    fun inNestArea() = IslandType.THE_END.isInIsland() && nestAreaPattern.matches(LorenzUtils.skyBlockArea)

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        chatEyePlacedPattern.matchMatcher(event.message.removeColor()) {
            yourEyesPlaced += 1
            ChatUtils.chat("You placed a Summoning Eye! ($yourEyesPlaced/8)")
        }
        chatEyePlacedFinalPattern.matchMatcher(event.message.removeColor()) {
            yourEyesPlaced += 1
            ChatUtils.chat("You placed a Summoning Eye! Brace Yourselves! ($yourEyesPlaced/8)")
        }

        chatEyeRecoveredPattern.matchMatcher(event.message.removeColor()) {
            yourEyesPlaced -= 1
            ChatUtils.chat("You recovered a Summoning Eye! ($yourEyesPlaced/8)")
        }

        chatSpawnPattern.matchMatcher(event.message.removeColor()) {
            currentType = group("type")
            DragonProfitTracker.BucketData().eyesPlaced += yourEyesPlaced
        }
        chatDeath.matchMatcher(event.message.removeColor()) {
            reset()
        }
    }

    fun reset() {
        currentType = null
        currentHp = null
        yourDamage = null
        yourEyesPlaced = 0
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
