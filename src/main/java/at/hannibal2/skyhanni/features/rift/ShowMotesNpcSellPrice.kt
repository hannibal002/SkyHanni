package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI.motesNpcPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShowMotesNpcSellPrice {

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!isEnabled()) return

        val itemStack = event.itemStack ?: return

        val motesPerItem = itemStack.motesNpcPrice() ?: return
        val size = itemStack.stackSize
        if (size > 1) {
        val motes = motesPerItem * size
            event.toolTip.add("§6NPC price: §d${motes.addSeparators()} Motes §7($size x §d${motesPerItem.addSeparators()} Motes§7)")
        } else {
            event.toolTip.add("§6NPC price: §d${motesPerItem.addSeparators()} Motes")
        }
    }

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.config.showMotesPrice
}
