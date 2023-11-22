package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

typealias SlotNumber = Int
typealias ItemName = String
typealias ItemData = Pair<ItemName, Int>

class OwnInventoryData {
    private var items = mapOf<SlotNumber, ItemData>()
    private var dirty = false

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val packet = event.packet
        if (packet is S2FPacketSetSlot || packet is S0DPacketCollectItem) {
            dirty = true
        }
        if (packet is S2FPacketSetSlot) {
            val windowId = packet.func_149175_c()
            if (windowId == 0) {
                val item = packet.func_149174_e() ?: return
                OwnInventoryItemUpdateEvent(item).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (items.isEmpty()) {
            initInventory()
        }

        if (!dirty) return

        dirty = false
        for ((slot, itemStack) in InventoryUtils.getItemsInOwnInventoryWithNull().withIndex()) {
            val old = items[slot]
            val new = itemStack.itemToPair()
            if (old != new) {
                item(slot, new, itemStack)
            }
        }
    }

    private fun initInventory() {
        items = items.editCopy {
            for ((slot, itemStack) in InventoryUtils.getItemsInOwnInventoryWithNull().withIndex()) {
                this[slot] = itemStack.itemToPair()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        items = emptyMap()
    }

    private fun ItemStack?.itemToPair(): ItemData = this?.let { (name ?: "null") to stackSize } ?: Pair("null", 0)

    private fun item(slot: SlotNumber, new: ItemData, itemStack: ItemStack?) {
        val (oldItem, oldAmount) = items[slot] ?: Pair("null", 0)
        val (name, amount) = new

        if (name == oldItem) {
            val diff = amount - oldAmount
            if (amount > oldAmount) {
                add(itemStack, diff)
            }
        } else {
            if (name != "null") {
                add(itemStack!!, amount)
            }
        }
        items = items.editCopy {
            this[slot] = new
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        BazaarApi.inBazaarInventory = false
        lastClose = System.currentTimeMillis()
    }

    private var lastClose = 0L

    private fun add(item_: ItemStack?, add: Int) {
        val item = item_ ?: return
        val diffClose = System.currentTimeMillis() - lastClose
        if (diffClose < 500) return

        val diffWorld = System.currentTimeMillis() - LorenzUtils.lastWorldSwitch
        if (diffWorld < 3_000) return

        val internalName = item.getInternalNameOrNull()

        item.name?.let {
            if (it == "§8Quiver Arrow") {
                return
            }
        }

        if (internalName == null) {
            LorenzUtils.debug("OwnInventoryData add is empty for: '${item.name}'")
            return
        }
        if (internalName.startsWith("MAP-")) return

        val (_, amount) = NEUItems.getMultiplier(internalName)
        if (amount > 1) return

        ItemAddInInventoryEvent(internalName, add).postAndCatch()
    }

}
