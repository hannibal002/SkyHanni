package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object OtherInventoryData {

    private var currentInventory: Inventory? = null
    private var acceptItems = false
    private var lateEvent: InventoryUpdatedEvent? = null

    val currentInventoryName: String
        get() = currentInventory?.title.orEmpty()

    @HandleEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        close()
    }

    @HandleEvent
    fun onPacketSent(event: PacketSentEvent) {
        if (event.packet is ServerboundContainerClosePacket) {
            close()
        }
    }

    fun close(title: String = currentInventoryName, reopenSameName: Boolean = false) {
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

    private val slotCountMap = mapOf(
        MenuType.ANVIL to 3,
        MenuType.BEACON to 1,
        MenuType.BLAST_FURNACE to 3,
        MenuType.BREWING_STAND to 5,
        MenuType.CARTOGRAPHY_TABLE to 2,
        MenuType.CRAFTING to 9,
        MenuType.ENCHANTMENT to 2,
        MenuType.FURNACE to 3,
        MenuType.GENERIC_3x3 to 9,
        MenuType.GENERIC_9x1 to 9,
        MenuType.GENERIC_9x2 to 18,
        MenuType.GENERIC_9x3 to 27,
        MenuType.GENERIC_9x4 to 36,
        MenuType.GENERIC_9x5 to 45,
        MenuType.GENERIC_9x6 to 54,
        MenuType.GRINDSTONE to 3,
        MenuType.HOPPER to 5,
        MenuType.LECTERN to 1,
        MenuType.LOOM to 3,
        MenuType.MERCHANT to 3,
        MenuType.SHULKER_BOX to 27,
        MenuType.SMITHING to 3,
        MenuType.SMOKER to 3,
        MenuType.STONECUTTER to 1,
    )

    @HandleEvent
    fun onInventoryDataReceiveEvent(event: PacketReceivedEvent) {
        val packet = event.packet

        if (packet is ClientboundContainerClosePacket) {
            close()
        }

        if (packet is ClientboundOpenScreenPacket) {
            val title = packet.title.unformattedTextCompat()
            val windowId = packet.containerId
            val handlerType = packet.type
            val slotCount = slotCountMap[handlerType] ?: ErrorManager.skyHanniError("Unknown screen handler type!", "screenName" to title)
            close(reopenSameName = title == currentInventory?.title)

            currentInventory = Inventory(windowId, title, slotCount)
            acceptItems = true
        }

        if (packet is ClientboundContainerSetSlotPacket) {
            if (!acceptItems) {
                currentInventory?.let {
                    if (it.windowId != packet.containerId) return

                    val slot = packet.slot
                    if (slot < it.slotCount) {
                        val itemStack = packet.item
                        if (itemStack.isNotEmpty()) {
                            it.items[slot] = itemStack
                            lateEvent = InventoryUpdatedEvent(it)
                        }
                    }
                }
                return
            }
            currentInventory?.let {
                if (it.windowId != packet.containerId) return

                val slot = packet.slot
                if (slot < it.slotCount) {
                    val itemStack = packet.item
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
        Minecraft.getInstance().execute {
            InventoryFullyOpenedEvent(inventory).post()
            inventory.fullyOpenedOnce = true
            InventoryUpdatedEvent(inventory).post()
            acceptItems = false
        }
    }

    class Inventory(
        val windowId: Int,
        val title: String,
        val slotCount: Int,
        val items: MutableMap<Int, ItemStack> = mutableMapOf(),
        var fullyOpenedOnce: Boolean = false,
    )
}
