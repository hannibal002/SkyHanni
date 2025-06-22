package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.test.command.ErrorManager
//$$ import net.minecraft.screen.ScreenHandlerType
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

    //#if MC > 1.21
    //$$ private val slotCountMap = mapOf(
    //$$    ScreenHandlerType.GENERIC_9X1 to 9,
    //$$    ScreenHandlerType.GENERIC_9X2 to 18,
    //$$    ScreenHandlerType.GENERIC_9X3 to 27,
    //$$    ScreenHandlerType.GENERIC_9X4 to 36,
    //$$    ScreenHandlerType.GENERIC_9X5 to 45,
    //$$    ScreenHandlerType.GENERIC_9X6 to 54,
    //$$    ScreenHandlerType.GENERIC_3X3 to 9,
    //$$    ScreenHandlerType.CRAFTING to 9,
    //$$    ScreenHandlerType.FURNACE to 3,
    //$$    ScreenHandlerType.BLAST_FURNACE to 3,
    //$$    ScreenHandlerType.SMOKER to 3,
    //$$    ScreenHandlerType.ANVIL to 3,
    //$$    ScreenHandlerType.BREWING_STAND to 5,
    //$$    ScreenHandlerType.SHULKER_BOX to 27,
    //$$    ScreenHandlerType.BEACON to 1,
    //$$ )
    //#endif

    @HandleEvent
    fun onInventoryDataReceiveEvent(event: PacketReceivedEvent) {
        val packet = event.packet

        if (packet is S2EPacketCloseWindow) {
            close()
        }

        if (packet is S2DPacketOpenWindow) {
            val title = packet.windowTitle.unformattedText
            val windowId = packet.windowId
            //#if MC < 1.21
            val slotCount = packet.slotCount
            //#else
            //$$ val handlerType = packet.screenHandlerType
            //$$ val slotCount = slotCountMap[handlerType] ?: ErrorManager.skyHanniError("Unknown screen handler type!", "screenName" to title)
            //#endif
            close(reopenSameName = title == currentInventory?.title)

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
                        if (itemStack.isNotEmpty()) {
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
                    if (itemStack.isNotEmpty()) {
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
