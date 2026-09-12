package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTooltip
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.garden.visitor.VisitorPriceCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingProfitTrackerVisitors {

    private val carrotColoredVinylSet = "CARROT_COLORED_VINYL_SET".toInternalName()

    private data class PendingVisitorVinylGift(
        val slotId: Int,
        val visitorName: String,
        val created: SimpleTimeMark,
    )

    /**
     * REGEX-TEST: +20 Copper
     */
    private val visitorCopperPattern by FarmingProfitTracker.patternGroup.pattern(
        "visitor.copper",
        "[+](?<amount>.*) Copper",
    )

    private var lastVisitorAccept = SimpleTimeMark.farPast()
    private var pendingVisitorVinylGift: PendingVisitorVinylGift? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onVisitorAccept(event: VisitorAcceptEvent) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.VISITORS)) return
        FarmingProfitTracker.modify {
            it.visitorsServed++
        }
        for ((internalName, amount) in event.visitor.shoppingList) {
            FarmingProfitTracker.addTrackedItem(TrackedSource.VISITORS, internalName, -amount.toLong(), message = false)
        }
        for (internalName in event.visitor.allRewards) {
            FarmingProfitTracker.addTrackedItem(TrackedSource.VISITORS, internalName, 1L, message = false)
        }
        lastVisitorAccept = SimpleTimeMark.now()
        FarmingProfitTracker.markActivity()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!VisitorApi.inInventory) return
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.VISITORS)) return
        val item = event.item ?: return
        if (!item.isCarrotColoredVinylSetGiveButton()) return
        val visitor = VisitorApi.getVisitor(VisitorApi.lastClickedNpc) ?: return
        val slotId = event.slot?.index ?: event.slotId
        pendingVisitorVinylGift = PendingVisitorVinylGift(slotId, visitor.visitorName, SimpleTimeMark.now())
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        val pending = pendingVisitorVinylGift ?: return
        if (!VisitorApi.inInventory || pending.created.passedSince() > 5.seconds) {
            pendingVisitorVinylGift = null
            return
        }
        val visitor = VisitorApi.getVisitor(VisitorApi.lastClickedNpc) ?: return
        if (visitor.visitorName != pending.visitorName) return
        val item = event.inventoryItems[pending.slotId] ?: return
        if (!item.isCharmedVisitorButton()) return
        if (!refreshVisitorOfferFromInventory(visitor, event)) return
        pendingVisitorVinylGift = null
        FarmingProfitTracker.modify {
            it.visitorVinylSetsGiven++
        }
        FarmingProfitTracker.addTrackedItem(TrackedSource.VISITORS, carrotColoredVinylSet, -1L, message = false)
        FarmingProfitTracker.markActivity()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.VISITORS)) return
        if (lastVisitorAccept.passedSince() > 1.seconds) return
        visitorCopperPattern.matchMatcher(event.cleanMessage.trim()) {
            val copper = group("amount").formatInt()
            val coinValue = VisitorPriceCalculator.calculateTotalReward(copper).roundToLong()
            FarmingProfitTracker.modify {
                it.visitorCopper += copper
            }
            FarmingProfitTracker.addTrackedItem(TrackedSource.VISITORS, SKYBLOCK_COIN, coinValue, message = false)
            FarmingProfitTracker.markActivity()
        }
    }

    private fun SafeItemStack.isCarrotColoredVinylSetGiveButton(): Boolean {
        if (hoverName.string != "Gift Vinyl Set") return false
        val lore = getLoreComponent().map { it.string }
        return "Gifts a Full Carrot-Colored Vinyl Set" in lore && "Click to gift!" in lore
    }

    private fun SafeItemStack.isCharmedVisitorButton(): Boolean {
        if (!hoverName.string.contains("This Visitor has been Charmed")) return false
        return getLoreComponent().any { it.string.contains("They will bring more rewards") }
    }

    private fun refreshVisitorOfferFromInventory(visitor: VisitorApi.Visitor, event: InventoryUpdatedEvent): Boolean {
        val offerItem = event.inventoryItems[VisitorApi.ACCEPT_SLOT] ?: return false
        if (offerItem.hoverName.string != "Accept Offer") return false
        visitor.offer = VisitorApi.VisitorOffer(offerItem)
        GardenVisitorTooltip.readVisitorOffer(visitor)
        return true
    }
}
