package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BazaarApi {
    private var loadedNpcPriceData = false

    companion object {
        val holder = BazaarDataHolder()
        var inBazaarInventory = false

        fun getBazaarDataByName(name: String): BazaarData? =
            NEUItems.getInternalNameOrNull(name)?.let { getBazaarDataByInternalName(it) }

        fun getBazaarDataByInternalName(internalName: String): BazaarData? {
            return if (isBazaarItem(internalName)) {
                holder.getData(internalName)
            } else null
        }

        fun isBazaarItem(stack: ItemStack) = isBazaarItem(stack.getInternalName())

        fun isBazaarItem(internalName: String): Boolean {
            return NEUItems.manager.auctionManager.getBazaarInfo(internalName) != null
        }

        fun searchForBazaarItem(displayName: String, amount: Int){
            if (NEUItems.neuHasFocus()) return
            if (LorenzUtils.noTradeMode) return
            if (LorenzUtils.inDungeons || LorenzUtils.inKuudraFight) return
            LorenzUtils.sendCommandToServer("bz ${displayName.removeColor()}")
            if (amount != -1) OSUtils.copyToClipboard(amount.toString())
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inBazaarInventory = checkIfInBazaar(event)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (!loadedNpcPriceData) {
            loadedNpcPriceData = true
            holder.start()
        }
    }

    private fun checkIfInBazaar(event: InventoryOpenEvent): Boolean {
        val returnItem = event.inventorySize - 5
        for ((slot, item) in event.inventoryItems) {
            if (slot == returnItem) {
                if (item.name?.removeColor().let { it == "Go Back" }) {
                    val lore = item.getLore()
                    if (lore.getOrNull(0)?.removeColor().let { it == "To Bazaar" }) {
                        return true
                    }
                }
            }
        }

        if (event.inventoryName.startsWith("Bazaar ➜ ")) return true
        return when (event.inventoryName) {
            "How many do you want?" -> true
            "How much do you want to pay?" -> true
            "Confirm Buy Order" -> true
            "Confirm Instant Buy" -> true
            "At what price are you selling?" -> true
            "Confirm Sell Offer" -> true
            "Order options" -> true

            else -> false
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inBazaarInventory = false
    }
}