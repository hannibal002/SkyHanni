package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration.Companion.seconds

object GetFromSackData {

    private val minimumDelay = 1.6.seconds

    private val queue: Queue<PrimitiveItemStack> = LinkedList()
    private val inventoryMap = mutableMapOf<Int, List<PrimitiveItemStack>>()

    private var lastTimeOfCommand = SimpleTimeMark.farPast()

    /** Do not use this use [GetFromSackAPI.get] instead*/
    fun addToQueue(items: List<PrimitiveItemStack>) = queue.addAll(items)

    /** Do not use this use [GetFromSackAPI.slot] instead*/
    fun addToInventory(items: List<PrimitiveItemStack>, slotId: Int) = inventoryMap.put(slotId, items)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (queue.isNotEmpty() && lastTimeOfCommand.passedSince() >= minimumDelay) {
            val item = queue.poll()
            lastTimeOfCommand = LorenzUtils.timeWhenNewQueuedUpCommandExecutes
            LorenzUtils.sendCommandToServer("gfs ${item.name.asString()} ${item.amount}")
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inventoryMap.clear()
    }

    @SubscribeEvent
    fun onSlotClicked(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.clickedButton != 1) return // right click
        addToQueue(inventoryMap[event.slotId] ?: return)
        inventoryMap.remove(event.slotId)
        event.isCanceled = true
    }
}
