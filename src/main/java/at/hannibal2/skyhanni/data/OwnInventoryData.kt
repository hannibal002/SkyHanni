package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.isBazaarItem
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
                itemStack?.let {
                    calculateDifference(slot, new, it)
                }
                items = items.editCopy {
                    this[slot] = new
                }
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

    private fun calculateDifference(slot: SlotNumber, new: ItemData, itemStack: ItemStack) {
        val (oldItem, oldAmount) = items[slot] ?: Pair("null", 0)
        val (name, amount) = new

        if (name == oldItem) {
            val diff = amount - oldAmount
            if (diff > 0) {
                addItem(itemStack, diff)
            }
        } else {
            if (name != "null") {
                addItem(itemStack, amount)
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        val item = Minecraft.getMinecraft().thePlayer.inventory.itemStack ?: return
        val internalNameOrNull = item.getInternalNameOrNull() ?: return
        ignoreItem(500.milliseconds) { it == internalNameOrNull }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (BazaarApi.inBazaarInventory) {
            ignoreItem(500.milliseconds) { it.isBazaarItem() }
        }
    }

    private fun ignoreItem(duration: Duration, condition: (NEUInternalName) -> Boolean) {
        ignoredItemsUntil.add(IgnoredItem(condition, SimpleTimeMark.now() + duration))
    }

    private val ignoredItemsUntil = mutableListOf<IgnoredItem>()

    class IgnoredItem(val condition: (NEUInternalName) -> Boolean, val blockedUntil: SimpleTimeMark)

    private fun addItem(item: ItemStack, add: Int) {
        val diffWorld = System.currentTimeMillis() - LorenzUtils.lastWorldSwitch
        if (diffWorld < 3_000) return

        item.name?.let {
            if (it == "§8Quiver Arrow") {
                return
            }
        }

        val internalName = item.getInternalNameOrNull() ?: run {
            LorenzUtils.debug("OwnInventoryData add is null for: '${item.name}'")
            return
        }

        ignoredItemsUntil.removeIf { it.blockedUntil.isInPast() }
        if (ignoredItemsUntil.any { it.condition(internalName) }) return

        if (internalName.startsWith("MAP-")) return

        ItemAddInInventoryEvent(internalName, add).postAndCatch()
        LorenzUtils.debug("added item internalName: $internalName")
    }
}
