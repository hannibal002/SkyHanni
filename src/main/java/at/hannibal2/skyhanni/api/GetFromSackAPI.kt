package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.data.SackStatus
import at.hannibal2.skyhanni.data.jsonobjects.repo.SacksJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration.Companion.seconds

object GetFromSackAPI {

    private val commands = arrayOf("gfs", "getfromsacks")

    fun getFromSack(item: PrimitiveItemStack) = getFromSack(listOf(item))

    fun getFromSack(items: List<PrimitiveItemStack>) = addToQueue(items)

    fun getFromChatMessageSackItems(
        item: PrimitiveItemStack,
        text: String = "Click here to grab §9x${item.amount} ${item.name.asString()}§e from sacks!"
    ) = LorenzUtils.clickableChat(text, "${commands.first()} ${item.name.asString()} ${item.amount}")

    fun getFromSlotSackItems(items: List<PrimitiveItemStack>, slotIndex: Int) = addToInventory(items, slotIndex)

    fun Slot.getFromSack(items: List<PrimitiveItemStack>) = getFromSlotSackItems(items, slotIndex)

    private val config get() = SkyHanniMod.feature.inventory

    private val minimumDelay = 1.7.seconds

    private val queue: Queue<PrimitiveItemStack> = LinkedList()
    private val inventoryMap = mutableMapOf<Int, List<PrimitiveItemStack>>()

    private var lastTimeOfCommand = SimpleTimeMark.farPast()

    var sackList = emptyList<NEUInternalName>()
        private set

    private fun addToQueue(items: List<PrimitiveItemStack>) = queue.addAll(items)

    private fun addToInventory(items: List<PrimitiveItemStack>, slotId: Int) = inventoryMap.put(slotId, items)

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
        if (event.clickedButton != 1) return // filter none right clicks
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

    @SubscribeEvent
    fun onMessageToServer(event: MessageSendToServerEvent) {
        if (!config.queuedGFS) return
        val message = event.message.split(" ")
        if (message.firstOrNull()
                ?.let { commands.contains(it) } != true // null or false == return
        ) return
        event.isCanceled = true
        commandHandler(message.drop(1))
    }

    fun commandHandler(args: List<String>) {
        if (!config.queuedGFS) {
            LorenzUtils.sendCommandToServer("gfs ${args.joinToString(" ")}")
            return
        }

        if (args.size != 2) {
            LorenzUtils.userError("Missing arguments! Usage: /getfromsacks <name/id> <amount>")
            return
        }

        val item = args[0].asInternalName()

        if (!sackList.contains(item)) {
            LorenzUtils.userError("Couldn't find an item with this name or identifier!")
            return
        }

        val amountString = args[1]

        if (!amountString.isInt()) {
            LorenzUtils.userError("Invalid amount!")
            return
        }

        val amount = amountString.toInt()

        if (config.bazaarGFS && !LorenzUtils.noTradeMode) {
            val sackInfo = SackAPI.fetchSackItem(item)
            if (sackInfo.getStatus() != SackStatus.CORRECT && sackInfo.getStatus() != SackStatus.ALRIGHT) {
                LorenzUtils.clickableChat(
                    "Unsure if items are available in Sack, §kCLICK §r§eto open bazaar", "bz ${item.asString()}"
                )
                getFromSack(item.makePrimitiveStack(amount))
                return
            }
            val sackAmount = sackInfo.amount.toInt()
            if (sackAmount < amount) {
                val diff = amount - sackAmount
                if (sackAmount > 0) {
                    getFromSack(item.makePrimitiveStack(sackAmount))
                }
                LorenzUtils.clickableChat(
                    "§kCLICK HERE §r§eto get the remaining §ax${diff} §ffrom bazaar", "bz ${item.asString()}"
                )
                return
            }
        }

        getFromSack(item.makePrimitiveStack(amount))
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackList = event.getConstant<SacksJson>("Sacks").sackList.map { it.replace(" ", "_").asInternalName() }
    }
}
