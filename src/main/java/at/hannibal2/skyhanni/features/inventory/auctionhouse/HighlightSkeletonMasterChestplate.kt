package at.hannibal2.skyhanni.features.inventory.auctionhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.slots
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonTier
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getStatBoostPercentage

@SkyHanniModule
object HighlightSkeletonMasterChestplate {

    private val config get() = SkyHanniMod.feature.inventory.auctions.skeletonMasterChestplateHighlight
    private val auctionMenu = InventoryDetector(checkInventoryName = { it.startsWith("Auctions") })
    private val SKELETON_MASTER_CHESTPLATE = "SKELETON_MASTER_CHESTPLATE".toInternalName()

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!auctionMenu.isInside()) return
        if (!isEnabled()) return
        for (slot in event.gui.slots()) {
            if (slot.item.getInternalName() != SKELETON_MASTER_CHESTPLATE) continue
            if (isGoodChestplate(slot.item) && config.highlightGoodChestplate) slot.highlight(config.goodColor)
            else if (config.highlightBadChestplate) slot.highlight(config.badColor)
        }
    }

    private fun isGoodChestplate(item: SafeItemStack): Boolean =
        (item.getDungeonTier() == 10 && item.getStatBoostPercentage() == 50)

    fun isEnabled(): Boolean = (config.highlightBadChestplate || config.highlightGoodChestplate)

    fun shouldOtherHighlightIgnore(internalName: NeuInternalName): Boolean = internalName == SKELETON_MASTER_CHESTPLATE && isEnabled()
}
