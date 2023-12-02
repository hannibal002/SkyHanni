package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PacketEvent
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object OtherInventoryData {
    private var currentInventory: Inventory? = null
    private var acceptItems = false
    private var lateEvent: InventoryUpdatedEvent? = null

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        close()
    }

    fun close(reopenSameName: Boolean = false) {
        currentInventory?.let {
            InventoryCloseEvent(it, reopenSameName).postAndCatch()
            currentInventory = null
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        lateEvent?.let {
            it.postAndCatch()
            lateEvent = null
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet is S2EPacketCloseWindow) {
            close()
        }

        if (packet is S2DPacketOpenWindow) {
            val windowId = packet.windowId
            val title = packet.windowTitle.unformattedText
            val slotCount = packet.slotCount
            val reopenSameName = title == currentInventory?.title
            close(reopenSameName)

            currentInventory = Inventory(windowId, title, slotCount)
            acceptItems = true
        }

        if (packet is S2FPacketSetSlot) {
            if (!acceptItems) {
                currentInventory?.let {
                    if (it.windowId != packet.func_149175_c()) return

                    val slot = packet.func_149173_d()
                    if (slot < it.slotCount) {
                        val itemStack = packet.func_149174_e()
                        if (itemStack != null) {
                            it.items[slot] = itemStack
                            lateEvent = InventoryUpdatedEvent(it)
                        }
                    }
                }
                return
            }
            currentInventory?.let {
                if (it.windowId != packet.func_149175_c()) return

                val slot = packet.func_149173_d()
                if (slot < it.slotCount) {
                    val itemStack = packet.func_149174_e()
                    if (itemStack != null) {
                        it.items[slot] = itemStack
                    }
                } else {
                    done(it)
                    return
                }

                if (it.items.size == it.slotCount) {
                    done(it)
                }
            }
        }
    }

    private fun done(inventory: Inventory) {
        InventoryFullyOpenedEvent(inventory).postAndCatch()
        InventoryUpdatedEvent(inventory).postAndCatch()
        acceptItems = false
    }

    class Inventory(
        val windowId: Int,
        val title: String,
        val slotCount: Int,
        val items: MutableMap<Int, ItemStack> = mutableMapOf(),
    )
}
