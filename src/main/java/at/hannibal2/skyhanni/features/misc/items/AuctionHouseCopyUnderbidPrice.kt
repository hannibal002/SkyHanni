package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AuctionHouseCopyUnderbidPrice {
    private val config get() = SkyHanniMod.feature.inventory

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (!event.fullyOpenedOnce) return
        if (event.inventoryName != "Create BIN Auction") return
        val item = event.inventoryItems[13] ?: return

        val internalName = item.getInternalName()
        if (internalName == NEUInternalName.NONE) return

        val price = internalName.getPrice().toLong()
        if (price <= 0) {
            OSUtils.copyToClipboard("")
            return
        }
        val newPrice = price * item.stackSize - 1
        OSUtils.copyToClipboard("$newPrice")
        LorenzUtils.chat("Set §e${newPrice.addSeparators()} §eto clipboard. (Copy Underbid Price)")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.copyUnderbidPrice
}
