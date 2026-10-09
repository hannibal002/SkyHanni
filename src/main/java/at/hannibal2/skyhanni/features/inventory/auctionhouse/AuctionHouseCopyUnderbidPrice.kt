package at.hannibal2.skyhanni.features.inventory.auctionhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getPetInternalNameWithLevel
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object AuctionHouseCopyUnderbidPrice {

    private val config get() = SkyHanniMod.feature.inventory.auctions

    private val patternGroup = RepoPattern.group("auctions.underbid")

    /**
     * REGEX-TEST: Buy it now: 1,000,000,000 coins
     * REGEX-TEST: Starting bid: 200,000,000 coins
     * REGEX-TEST: Top bid: 220,000 coins
     */
    private val auctionPricePattern by patternGroup.pattern(
        "price.colorless",
        "(?:Buy it now|Starting bid|Top bid): (?<coins>[0-9,]+) coins",
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

    private var lastCopiedItem: NeuInternalName? = null

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.autoCopyUnderbidPrice) return
        if (!event.fullyOpenedOnce) return
        if (event.inventoryName != "Create BIN Auction") return
        val item = event.inventoryItems[13] ?: return

        val internalName = item.getPetInternalNameWithLevel()
        if (internalName == lastCopiedItem) return
        lastCopiedItem = internalName
        if (internalName == NeuInternalName.NONE) return

        val price = internalName.getPrice().toLong()
        if (price <= 0) {
            OSUtils.copyToClipboard("")
            return
        }
        copyPrice(price * item.count)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!config.copyUnderbidKeybind.isKeyHeld()) return
        if (!allowedInventoriesPattern.matches(InventoryUtils.openInventoryName())) return
        val stack = event.stackUnderCursor ?: return

        auctionPricePattern.firstMatcher(stack.getCleanLore()) {
            copyPrice(group("coins").formatLong())
        }
    }

    private fun copyPrice(price: Long) {
        val underbidPrice = price - 1
        OSUtils.copyToClipboard("$underbidPrice")
        ChatUtils.chat(
            "Copied ${underbidPrice.addSeparators()} to clipboard. (Copy Underbid Price)",
        )
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "inventory.copyUnderbidPrice", "inventory.auctions.autoCopyUnderbidPrice")
    }
}
