package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CompactExperimentRewards {

    private val enabled get() = SkyHanniMod.feature.chat.compactRewards

    private var gainedRewards = mutableListOf<String>()
    private var lastTimeTableOpened = SimpleTimeMark.farPast()
    private var currentMessage = ""

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Superpairs Rewards
     */
    private val patternGroup = RepoPattern.group("chat.experiments.compact")
    val experimentInventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) (?:\\(.+\\)|➜ Stakes|Rewards)|Experimentation Table",
    )

    /**
     * REGEX-TEST: §eYou claimed the §r§dUltrasequencer §r§erewards!
     * REGEX-TEST: §eYou claimed the §r§cUltrasequencer §r§erewards!
     */
    val claimMessagePattern by patternGroup.pattern(
        "message",
        "(?<message>§eYou claimed the §r§.\\S+ §r§erewards!)",
    )

    /**
     * REGEX-TEST: §8 +§r§3600k Enchanting Exp
     * REGEX-TEST:  §r§8+§r§3132k Enchanting Exp
     * REGEX-TEST:  §r§8+§r§aThunderlord V
     * REGEX-TEST:  §r§8+§r§3143k Enchanting Exp
     * REGEX-TEST:  §r§8+§r§aGrand Experience Bottle
     * REGEX-TEST:  §r§8+§r§aCaster V
     */
    private val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^(?:§8 \\+| §r§8\\+)(?<reward>.*)\$",
    )

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (enabled && experimentInventoriesPattern.matches(InventoryUtils.openInventoryName())) {
            lastTimeTableOpened = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!enabled || lastTimeTableOpened.passedSince() >= 3.seconds || event.blockedReason != "") return

        event.message.let { message ->
            claimMessagePattern.matchMatcher(message) {
                currentMessage = group("message")
                event.blockedReason = "COMPACT_REWARDS"
                return
            }
            experimentsDropPattern.matchMatcher(message) {
                val reward = group("reward")

                gainedRewards.add(reward)
                event.blockedReason = "COMPACT_REWARDS"

                DelayedRun.runDelayed(100.milliseconds) {
                    if (gainedRewards.last() == reward && currentMessage != "") {

                        val expList = mutableListOf<String>().apply {
                            gainedRewards.forEach {
                                add("§8+$it")
                            }
                        }

                        ChatUtils.hoverableChat(currentMessage, expList, null, false)
                        gainedRewards.clear()
                        currentMessage = ""
                    }
                }
            }
        }
    }
}
