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
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CompactExperimentRewards {

    private val enabled get() = SkyHanniMod.feature.chat.compactEnchantingExp

    private var gainedRewards = mutableListOf<String>()
    private var lastTimeTableOpened = SimpleTimeMark.farPast()

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Superpairs Rewards
     */
    private val patternGroup = RepoPattern.group("chat.experiments.compact")
    val experimentInventoriesPattern by patternGroup.pattern(
        "inventories",
        "Superpairs (?:\\(.+\\)|Rewards)",
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
        "^.[^+]+\\+(?<reward>.*)\$",
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

        if (event.message.removeColor() == "You claimed the Superpairs rewards!") {
            event.blockedReason = "COMPACT_REWARDS"
            return
        }

        event.message.let { message ->
            experimentsDropPattern.matchMatcher(message) {
                val reward = group("reward")

                gainedRewards.add(reward)
                event.blockedReason = "COMPACT_REWARDS"

                DelayedRun.runDelayed(100.milliseconds) {
                    if (gainedRewards.last() == reward) {
                        val chatMessage = "§eYou claimed the §dSuperpairs §erewards!"

                        val expList = mutableListOf<String>().apply {
                            gainedRewards.forEach {
                                add("§8+$it")
                            }
                        }

                        ChatUtils.hoverableChat(chatMessage, expList, null, false)
                        gainedRewards.clear()
                    }
                }
            }
        }
    }
}
