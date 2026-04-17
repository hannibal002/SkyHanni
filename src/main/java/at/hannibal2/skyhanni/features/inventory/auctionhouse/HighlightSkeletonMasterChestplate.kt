package at.hannibal2.skyhanni.features.inventory.auctionhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonTier
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getStatBoostPercentage
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object HighlightSkeletonMasterChestplate {

    private val config get() = SkyHanniMod.feature.inventory.auctions.skeletonMaster
    private val auctionMenu = InventoryDetector(checkInventoryName = { it.startsWith("Auctions") })

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!auctionMenu.isInside()) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.item.getInternalName() != "SKELETON_MASTER_CHESTPLATE".toInternalName()) continue
            if (isGoodChestplate(slot.item) && config.highlightGoodChestplate) slot.highlight(config.goodColor)
            else if (config.highlightBadChestplate) slot.highlight(config.badColor)
        }
    }

    private fun isGoodChestplate(item: ItemStack): Boolean =
        (item.getDungeonTier() == 10 && item.getStatBoostPercentage() == 50)
}
