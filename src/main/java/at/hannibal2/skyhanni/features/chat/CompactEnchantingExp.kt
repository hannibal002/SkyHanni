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
object CompactEnchantingExp {

    private val enabled get() = SkyHanniMod.feature.chat.compactEnchantingExp

    private var gainedExperience = mutableListOf<Int>()
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
     * REGEX-TEST:  +141k Enchanting Exp
     * REGEX-TEST:  +134k Enchanting Exp
     */
    private val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "^ \\+(?<amount>\\d+)k Enchanting Exp\$",
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

        event.message.removeColor().let { message ->
            enchantingExpPattern.matchMatcher(message) {
                val amount = group("amount").toInt()

                gainedExperience.add(amount)
                event.blockedReason = "COMPACT_EXP"

                DelayedRun.runDelayed(100.milliseconds) {
                    if (gainedExperience.last() == amount) {
                        val totalExp = gainedExperience.sum()
                        val maxExp = gainedExperience.maxOrNull()
                        val chatMessage = " §8+§3${totalExp}k Enchanting Exp"

                        val expList = mutableListOf<String>().apply {
                            add("§3${maxExp}k §8(§7base§8)")
                            gainedExperience.removeIf { it == maxExp }
                            gainedExperience.forEach {
                                add(" §2+ §3${it}k §8(§7pair§8)")
                            }
                        }

                        ChatUtils.hoverableChat(chatMessage, expList, null, false)
                        gainedExperience.clear()
                    }
                }
            }
        }
    }
}
