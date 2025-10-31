package at.hannibal2.hanni.features.inventory.auctionhouse

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.events.InventoryUpdatedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.compat.stackUnderCursor
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object AuctionHouseCopyUnderbidPrice {

    private val config get() = HanniMod.feature.inventory.auctions

    private val patternGroup = RepoPattern.group("auctions.underbid")

    /**
     * REGEX-TEST: §7Buy it now: §61,000,000,000 coins
     * REGEX-TEST: §7Starting bid: §6200,000,000 coins
     * REGEX-TEST: §7Top bid: §6220,000 coins
     */
    private val auctionPricePattern by patternGroup.pattern(
        "price",
        "§7(?:Buy it now|Starting bid|Top bid): §6(?<coins>[0-9,]+) coins",
    )

    /**
     * REGEX-TEST: Auctions Browser
     * REGEX-TEST: Manage Auctions
     * REGEX-TEST: Auctions: "aaa"
     */
    private val allowedInventoriesPattern by patternGroup.pattern(
        "allowedinventories",
        "Auctions Browser|Manage Auctions|Auctions: \".*\"?",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.autoCopyUnderbidPrice) return
        if (!event.fullyOpenedOnce) return
        if (event.inventoryName != "Create BIN Auction") return
        val item = event.inventoryItems[13] ?: return

        val internalName = item.getInternalName()
        if (internalName == NeuInternalName.NONE) return

        val price = internalName.getPrice().toLong()
        if (price <= 0) {
            OSUtils.copyToClipboard("")
            return
        }
        val newPrice = price * item.stackSize - 1
        OSUtils.copyToClipboard("$newPrice")
        ChatUtils.chat("Copied ${newPrice.addSeparators()} to clipboard. (Copy Underbid Price)")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.copyUnderbidKeybind.isKeyHeld()) return
        if (!allowedInventoriesPattern.matches(InventoryUtils.openInventoryName())) return
        val stack = stackUnderCursor() ?: return

        auctionPricePattern.firstMatcher(stack.getLore()) {
            val underbid = group("coins").formatLong() - 1
            OSUtils.copyToClipboard("$underbid")
            ChatUtils.chat("Copied ${underbid.addSeparators()} to clipboard.")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "inventory.copyUnderbidPrice", "inventory.auctions.autoCopyUnderbidPrice")
    }
}
