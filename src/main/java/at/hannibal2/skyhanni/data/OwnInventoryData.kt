package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class OwnInventoryData {

    private var itemAmounts = mapOf<NEUInternalName, Int>()
    private var dirty = false
    private val sackToInventoryChatPattern by RepoPattern.pattern(
        "data.owninventory.chat.movedsacktoinventory",
        "§aMoved §r§e\\d* (?<name>.*)§r§a from your Sacks to your inventory."
    )

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onItemPickupReceivePacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val packet = event.packet
        if (packet is S2FPacketSetSlot || packet is S0DPacketCollectItem) {
            dirty = true
        }
        if (packet is S2FPacketSetSlot) {
            val windowId = packet.func_149175_c()
            if (windowId == 0) {
                val slot = packet.func_149173_d()
                val item = packet.func_149174_e() ?: return
                DelayedRun.runNextTick {
                    OwnInventoryItemUpdateEvent(item, slot).postAndCatch()
                }
            }
        }
    }

    @SubscribeEvent
    fun onClickEntity(event: PacketEvent.SendEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val packet = event.packet

        if (packet is C0EPacketClickWindow) {
            dirty = true
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (itemAmounts.isEmpty()) {
            itemAmounts = getCurrentItems()
        }

        if (!dirty) return
        dirty = false

        val map = getCurrentItems()
        for ((internalName, amount) in map) {
            calculateDifference(internalName, amount)
        }
        itemAmounts = map
    }

    private fun getCurrentItems(): MutableMap<NEUInternalName, Int> {
        val map = mutableMapOf<NEUInternalName, Int>()
        for (itemStack in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = itemStack.getInternalNameOrNull() ?: continue
            map.addOrPut(internalName, itemStack.stackSize)
        }
        return map
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        itemAmounts = emptyMap()
    }

    private fun calculateDifference(internalName: NEUInternalName, newAmount: Int) {
        val oldAmount = itemAmounts[internalName] ?: 0

        val diff = newAmount - oldAmount
        if (diff > 0) {
            addItem(internalName, diff)
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        val item = Minecraft.getMinecraft().thePlayer.inventory.itemStack ?: return
        val internalNameOrNull = item.getInternalNameOrNull() ?: return
        ignoreItem(500.milliseconds) { it == internalNameOrNull }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        ignoreItem(500.milliseconds) { true }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        sackToInventoryChatPattern.matchMatcher(event.message) {
            val name = group("name")
            ignoreItem(500.milliseconds) { it.itemName.contains(name) }
        }
    }

    private fun ignoreItem(duration: Duration, condition: (NEUInternalName) -> Boolean) {
        ignoredItemsUntil.add(IgnoredItem(condition, SimpleTimeMark.now() + duration))
    }

    private val ignoredItemsUntil = mutableListOf<IgnoredItem>()

    class IgnoredItem(val condition: (NEUInternalName) -> Boolean, val blockedUntil: SimpleTimeMark)

    private fun addItem(internalName: NEUInternalName, add: Int) {
        if (LorenzUtils.lastWorldSwitch.passedSince() < 3.seconds) return

        ignoredItemsUntil.removeIf { it.blockedUntil.isInPast() }
        if (ignoredItemsUntil.any { it.condition(internalName) }) {
            return
        }

        if (internalName.startsWith("MAP-")) return

        ItemAddInInventoryEvent(internalName, add).postAndCatch()
    }
}
