package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.*
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class InventoryData {
    private var currentInventory: Inventory? = null
    private var acceptItems = false
    private var lateEvent: LateInventoryOpenEvent? = null

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        close()
    }

    private fun close() {
        currentInventory?.let {
            InventoryCloseEvent(it).postAndCatch()
            currentInventory = null
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        lateEvent?.let {
            it.postAndCatch()
            lateEvent = null
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet is S2DPacketOpenWindow) {
            val windowId = packet.windowId
            val title = packet.windowTitle.unformattedText
            val slotCount = packet.slotCount
            close()

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
                            lateEvent = LateInventoryOpenEvent(it)
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
        InventoryOpenEvent(inventory).postAndCatch()
        acceptItems = false
    }

    class Inventory(
        val windowId: Int,
        val title: String,
        val slotCount: Int,
        val items: MutableMap<Int, ItemStack> = mutableMapOf(),
    )
}