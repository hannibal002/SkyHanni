package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BazaarCancelledBuyOrderClipboard {

    private val patternLastAmount = Pattern.compile("§a(?<amount>.*)§7x")
    private val patternCancelledMessage =
        "§6\\[Bazaar] §r§7§r§cCancelled! §r§7Refunded §r§6(?<coins>.*) coins §r§7from cancelling Buy Order!".toPattern()

    private var latestAmount: String? = null

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return

        val stack = event.itemStack ?: return
        val name = stack.name ?: return
        if (!name.contains("Cancel Order")) return

        for (line in stack.getLore()) {
            val matcher = patternLastAmount.matcher(line)
            if (matcher.find()) {
                latestAmount = matcher.group("amount")
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        patternCancelledMessage.matchMatcher(event.message) {
            event.blockedReason = "bazaar cancelled buy order clipbaord"
            val coins = group("coins")
            LorenzUtils.chat("Bazaar buy order cancelled. $latestAmount saved to clipboard. ($coins coins)")

            latestAmount?.let { OSUtils.copyToClipboard(it.replace(",", "")) }
            latestAmount = null
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.cancelledBuyOrderClipboard
}
