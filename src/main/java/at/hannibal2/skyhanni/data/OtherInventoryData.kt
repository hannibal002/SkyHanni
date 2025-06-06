package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
//#if MC > 1.21
//$$ import net.minecraft.network.packet.s2c.play.InventoryS2CPacket
//#endif

@SkyHanniModule
object OtherInventoryData {

    private var currentInventory: Inventory? = null
    private var acceptItems = false
    private var lateEvent: InventoryUpdatedEvent? = null

    @HandleEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        close()
    }

    fun close(title: String = InventoryUtils.openInventoryName(), reopenSameName: Boolean = false) {
        InventoryCloseEvent(title, reopenSameName).post()
        currentInventory = null
    }

    @HandleEvent
    fun onTick() {
        lateEvent?.let {
            it.post()
            lateEvent = null
        }
    }

    @HandleEvent
    fun onInventoryDataReceiveEvent(event: PacketReceivedEvent) {
        val packet = event.packet

        if (packet is S2EPacketCloseWindow) {
            close()
        }

        //#if MC < 1.21
        if (packet is S2DPacketOpenWindow) {
            //#else
            //$$ if (packet is InventoryS2CPacket) {
            //#endif
            //#if MC < 1.21
            val title = packet.windowTitle.unformattedText
            val windowId = packet.windowId
            val slotCount = packet.slotCount
            //#else
            //$$ val oldWindowId = currentInventory?.windowId
            //$$ val windowId = packet.syncId
            //$$ if (oldWindowId != windowId) {
            //$$    val title = InventoryUtils.openInventoryName()
            //$$    val slotCount = packet.contents.size
            //#endif
            close(reopenSameName = title == currentInventory?.title)

            currentInventory = Inventory(windowId, title, slotCount)
            acceptItems = true
            //#if MC > 1.21
            //$$    for ((i, stack) in packet.contents.withIndex()) {
            //$$        currentInventory?.items?.put(i, stack)
            //$$    }
            //$$    if (currentInventory != null) {
            //$$        InventoryFullyOpenedEvent(currentInventory!!).post()
            //$$        currentInventory!!.fullyOpenedOnce = true
            //$$        InventoryUpdatedEvent(currentInventory!!).post()
            //$$    }
            //$$ }
            //#endif
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
                    if (itemStack.orNull() != null) {
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
        InventoryFullyOpenedEvent(inventory).post()
        inventory.fullyOpenedOnce = true
        InventoryUpdatedEvent(inventory).post()
        acceptItems = false
    }

    class Inventory(
        val windowId: Int,
        val title: String,
        val slotCount: Int,
        val items: MutableMap<Int, ItemStack> = mutableMapOf(),
        var fullyOpenedOnce: Boolean = false,
    )
}
