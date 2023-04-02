package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.OwnInventorItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class OwnInventoryData {

    private var itemNames = mutableMapOf<Int, String>()
    private var itemAmount = mutableMapOf<Int, Int>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val packet = event.packet
        if (packet is S2FPacketSetSlot) {
            val windowId = packet.func_149175_c()
            if (windowId == 0) {
                val item = packet.func_149174_e() ?: return
                OwnInventorItemUpdateEvent(item).postAndCatch()
            }
        }
        if (packet is S2FPacketSetSlot) {
            val windowId = packet.func_149175_c()
            val item = packet.func_149174_e()
            val slot = packet.func_149173_d()
            if (windowId != 0) return
            val name = item?.name ?: "null"

            val oldItem = itemNames.getOrDefault(slot, "null")
            val oldAmount = itemAmount.getOrDefault(slot, 0)

            val amount = item?.stackSize ?: 0
            if (name == oldItem) {
                val diff = amount - oldAmount
                if (amount > oldAmount) {
                    add(item, diff)
                }
            } else {
                if (name != "null") {
                    add(item, amount)
                }
            }
            itemNames[slot] = name
            itemAmount[slot] = amount
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        BazaarApi.inBazaarInventory = false
        lastClose = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        lastWorldSwitch = System.currentTimeMillis()
    }

    private var lastClose = 0L
    private var lastWorldSwitch = 0L

    private fun add(item: ItemStack?, add: Int) {
        if (item == null) return

        val diffClose = System.currentTimeMillis() - lastClose
        if (diffClose < 500) return

        val diffWorld = System.currentTimeMillis() - lastWorldSwitch
        if (diffWorld < 3_000) return

        val internalName = item.getInternalName()
        val (_, amount) = NEUItems.getMultiplier(internalName)
        if (amount > 1) return

        if (internalName == "") {
            LorenzUtils.debug("OwnInventoryData add is empty for: '$internalName'")
            return
        }

        addMultiplier(internalName, add)
    }

    private fun addMultiplier(internalName: String, amount: Int) {
        CollectionAPI.addFromInventory(internalName, amount)
    }
}