package at.hannibal2.hanni.features.inventory.bazaar

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.chat.ShortenCoins.formatChatCoins
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatDouble
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object BazaarCancelledBuyOrderClipboard {

    private val patternGroup = RepoPattern.group("bazaar.cancelledorder")

    /**
     * REGEX-TEST: §6§7from §a50§7x §7missing items.
     * REGEX-TEST: §7§a22§7x §7missing items.
     * REGEX-TEST: §6coins §7from §a2,000§7x §7missing items.
     */
    private val lastAmountPattern by patternGroup.pattern(
        "lastamount",
        "(?:§6coins §7from |§6§7from |§7)§a(?<amount>.*)§7x §7missing items\\.",
    )
    private val cancelledMessagePattern by patternGroup.pattern(
        "cancelledmessage",
        "§6\\[Bazaar] §r§7§r§cCancelled! §r§7Refunded §r§6(?<coins>.*) coins §r§7from cancelling Buy Order!",
    )
    private val inventoryTitlePattern by patternGroup.pattern(
        "inventorytitle",
        "Order options",
    )

    private var latestAmount: Int? = null

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryTitlePattern.matches(event.inventoryName)) return
        val stack = event.inventoryItems[11] ?: return
        if (!stack.displayName.contains("Cancel Order")) return

        val lore = stack.getLore()
        lastAmountPattern.firstMatcher(lore) {
            latestAmount = group("amount").formatInt()
            return
        }

        // nothing to cancel
        if (lore.firstOrNull() == "§7Cannot cancel order while there are") {
            return
        }

        ErrorManager.logErrorStateWithData(
            "BazaarCancelledBuyOrderClipboard error",
            "lastAmountPattern can not find latestAmount",
            "lore" to lore,
        )
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        val coins = cancelledMessagePattern.matchMatcher(event.message) {
            group("coins").formatDouble()
        } ?: return

        val latestAmount = latestAmount ?: return
        event.blockedReason = "bazaar cancelled buy order clipboard"
        val lastClicked = BazaarApi.orderOptionProduct
            ?: ErrorManager.hanniError("Cancel buy order clipboard could not detect the last bazaar product.")

        val message = "Bazaar buy order cancelled. Click to re-order.\n" +
            "§e(§8${latestAmount.addSeparators()}x §r${lastClicked.repoItemName}§e for ${coins.formatChatCoins()} coins§e)"
        ChatUtils.clickableChat(
            message,
            onClick = {
                BazaarApi.searchForBazaarItem(lastClicked, latestAmount)
            },
        )
        OSUtils.copyToClipboard(latestAmount.toString())
        this.latestAmount = null
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && HanniMod.feature.inventory.bazaar.cancelledBuyOrderClipboard
}
