package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.jsonobjects.repo.SacksJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration.Companion.seconds

object GetFromSackData {

    private val minimumDelay = 1.7.seconds

    private val queue: Queue<PrimitiveItemStack> = LinkedList()
    private val inventoryMap = mutableMapOf<Int, List<PrimitiveItemStack>>()

    private var lastTimeOfCommand = SimpleTimeMark.farPast()

    var sackList = emptyList<NEUInternalName>()
        private set

    /** Do not use this use [GetFromSackAPI.get] instead*/
    fun addToQueue(items: List<PrimitiveItemStack>) = queue.addAll(items)

    /** Do not use this use [GetFromSackAPI.slot] instead*/
    fun addToInventory(items: List<PrimitiveItemStack>, slotId: Int) = inventoryMap.put(slotId, items)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (queue.isNotEmpty() && lastTimeOfCommand.passedSince() >= minimumDelay) {
            val item = queue.poll()
            LorenzUtils.sendCommandToServer("gfs ${item.name.asString()} ${item.amount}")
            lastTimeOfCommand = SimpleTimeMark.now()
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

    @SubscribeEvent
    fun onTooltipRender(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val list = inventoryMap[event.slot.slotIndex] ?: return
        event.toolTip.let { tip ->
            tip.add("")
            tip.add("§ePress right click to get from sack:")
            tip.addAll(list.map { "§ex" + it.amount.toString() + " " + it.name.asString() })
        }
    }

    fun commandHandler(args: Array<String>) {
        if (args.size != 2) {
            LorenzUtils.chat("§cMissing arguments! Usage: /getfromsacks <name/id> <amount>", prefix = false)
            return
        }

        val item = args[0].asInternalName()

        if (!sackList.contains(item)) {
            LorenzUtils.chat("§cCouldn't find an item with this name or identifier!", prefix = false)
            return
        }

        val amountString = args[1]

        if (!amountString.isInt()) {
            LorenzUtils.chat("§cInvalid amount!", prefix = false)
            return
        }

        GetFromSackAPI.get(item.makePrimitiveStack(amountString.toInt()))
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackList = event.getConstant<SacksJson>("Sacks").sackList.map { it.replace(" ", "_").asInternalName() }
    }
}
