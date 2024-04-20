package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarCancelledBuyOrderClipboard {

    private val patternGroup = RepoPattern.group("bazaar.cancelledorder")

    /**
     * REGEX-TEST: §6§7from §a50§7x §7missing items.
     */
    private val lastAmountPattern by patternGroup.pattern(
        "lastamount",
        "§6§7from §a(?<amount>.*)§7x §7missing items\\."
    )
    private val cancelledMessagePattern by patternGroup.pattern(
        "cancelledmessage",
        "§6\\[Bazaar] §r§7§r§cCancelled! §r§7Refunded §r§6(?<coins>.*) coins §r§7from cancelling Buy Order!"
    )
    private val inventoryTitlePattern by patternGroup.pattern(
        "inventorytitle",
        "Order options"
    )

    private var latestAmount: Int? = null

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryTitlePattern.matches(event.inventoryName)) return
        val stack = event.inventoryItems[11] ?: return
        if (!stack.name.contains("Cancel Order")) return

        stack.getLore().matchFirst(lastAmountPattern) {
            latestAmount = group("amount").formatInt()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val coins = cancelledMessagePattern.matchMatcher(event.message) {
            group("coins").formatInt().addSeparators()
        } ?: return

        val latestAmount = latestAmount ?: error("latest amount is null")
        event.blockedReason = "bazaar cancelled buy order clipboard"
        ChatUtils.chat("Bazaar buy order cancelled. ${latestAmount.addSeparators()} saved to clipboard. ($coins coins)")
        OSUtils.copyToClipboard(latestAmount.toString())
        this.latestAmount = null
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.bazaar.cancelledBuyOrderClipboard
}
