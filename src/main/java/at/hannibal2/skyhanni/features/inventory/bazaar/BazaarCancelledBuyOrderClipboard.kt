package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarCancelledBuyOrderClipboard {

    private val patternGroup = RepoPattern.group("bazaar.cancelledorder")
    private val lastAmountPattern by patternGroup.pattern(
        "lastamount",
        "§a(?<amount>.*)§7x"
    )
    private val cancelledMessagePattern by patternGroup.pattern(
        "cancelledmessage",
        "§6\\[Bazaar] §r§7§r§cCancelled! §r§7Refunded §r§6(?<coins>.*) coins §r§7from cancelling Buy Order!"
    )

    private var latestAmount: String? = null

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return

        val stack = event.itemStack
        if (!stack.name.contains("Cancel Order")) return

        stack.getLore().matchFirst(lastAmountPattern) {
            latestAmount = group("amount")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        cancelledMessagePattern.matchMatcher(event.message) {
            event.blockedReason = "bazaar cancelled buy order clipboard"
            val coins = group("coins")
            ChatUtils.chat("Bazaar buy order cancelled. $latestAmount saved to clipboard. ($coins coins)")

            latestAmount?.let { OSUtils.copyToClipboard(it.replace(",", "")) }
            latestAmount = null
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.bazaar.cancelledBuyOrderClipboard
}
