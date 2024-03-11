package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AuctionsHighlighter {

    private val config get() = SkyHanniMod.feature.inventory.auctions

    private val patternGroup = RepoPattern.group("auctions.highlight")
    private val bidderPattern by patternGroup.pattern(
        "bidder",
        "§7(?:Bidder|Buyer): (?<player>.*)"
    )
    private val endedPattern by patternGroup.pattern(
        "ended",
        "§7Status: §a(?:Sold|Ended)!"
    )
    private val buyItNowPattern by patternGroup.pattern(
        "buyitnow",
        "§7Buy it now: §6(?<coins>.*) coins"
    )

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.gui !is GuiChest) return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        when (chest.getInventoryName()) {
            "Your Bids" -> {
                if (!config.highlightBids) return

                for (slot in chest.inventorySlots) {
                    if (slot == null) continue
                    if (slot.slotNumber != slot.slotIndex) continue
                    val stack = slot.stack ?: continue

                    val lore = stack.getLore()

                    val playerName = LorenzUtils.getPlayerName()
                    val bidderName = lore.firstOrNull { bidderPattern.matches(it) }?.let {
                        bidderPattern.matchMatcher(it) {
                            group("player").removeColor().substringAfterLast(" ")
                        }
                    } ?: continue

                    if (lore.any { endedPattern.matches(it) }) {
                        val color = if (bidderName == playerName) LorenzColor.GREEN else LorenzColor.RED
                        slot highlight color
                        continue
                    }

                    if (bidderName != playerName) {
                        slot highlight LorenzColor.GOLD
                        continue
                    }
                }
            }

            "Manage Auctions" -> {
                if (!config.highlightAuctions) return

                for (slot in chest.inventorySlots) {
                    if (slot == null) continue
                    if (slot.slotNumber != slot.slotIndex) continue
                    val stack = slot.stack ?: continue

                    val lore = stack.getLore()
                    if (lore.any { it == "§7Status: §aSold!" }) {
                        slot highlight LorenzColor.GREEN
                        continue
                    }
                    if (lore.any { it == "§7Status: §cExpired!" }) {
                        slot highlight LorenzColor.RED
                        continue
                    }
                    if (config.highlightAuctionsUnderbid) {
                        for (line in lore) {
                            buyItNowPattern.matchMatcher(line) {
                                val coins = group("coins").formatLong()
                                stack.getInternalNameOrNull()?.getPriceOrNull()?.let {
                                    if (coins > it) {
                                        slot highlight LorenzColor.GOLD
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "inventory.highlightAuctions", "inventory.auctions.highlightAuctions")
        event.move(25, "inventory.highlightAuctionsUnderbid", "inventory.auctions.highlightAuctionsUnderbid")
    }
}
