package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest

@HanniModule
object AuctionsHighlighter {

    private val config get() = HanniMod.feature.inventory.auctions

    private val patternGroup = RepoPattern.group("auctions.highlight")

    /**
     * REGEX-TEST: §7Buy it now: §62,599,999,999 coins
     */
    val buyItNowPattern by patternGroup.pattern(
        "buyitnow",
        "§7Buy it now: §6(?<coins>.*) coins",
    )

    /**
     * REGEX-TEST: §7Starting bid: §6985,000,000 coins
     * REGEX-TEST: §7Top bid: §667 coins
     */
    val auctionPattern by patternGroup.pattern(
        "auction",
        "§7(?:Starting bid|Top bid): §6(?<coins>.*) coins",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightAuctions) return
        if (event.gui !is GuiChest) return

        val chest = event.container as ContainerChest
        if (InventoryUtils.openInventoryName() != "Manage Auctions") return

        for ((slot, stack) in chest.getUpperItems()) {
            val lore = stack.getLore()
            if (lore.any { it == "§7Status: §aSold!" }) {
                slot.highlight(config.soldColor)
                continue
            }
            if (lore.any { it == "§7Status: §cExpired!" }) {
                slot.highlight(config.expiredColor)
                continue
            }
            if (config.highlightAuctionsUnderbid) {
                buyItNowPattern.firstMatcher(lore) {
                    val coins = group("coins").formatLong()
                    EstimatedItemValueCalculator.getTotalPrice(stack)?.let { totalPrice ->
                        if (coins > totalPrice) {
                            slot.highlight(config.underbidColor)
                        }
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "inventory.highlightAuctions", "inventory.auctions.highlightAuctions")
        event.move(25, "inventory.highlightAuctionsUnderbid", "inventory.auctions.highlightAuctionsUnderbid")
    }
}
