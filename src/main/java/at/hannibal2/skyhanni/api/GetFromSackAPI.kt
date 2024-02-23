package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.SacksJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.isCommand
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Deque
import java.util.LinkedList
import kotlin.time.Duration.Companion.seconds

object GetFromSackAPI {
    private val config get() = SkyHanniMod.feature.inventory.gfs

    val commands = arrayOf("gfs", "getfromsacks")
    val commandsWithSlash = commands.map { "/$it" }

    private val patternGroup = RepoPattern.group("gfs.chat")
    private val fromSacksChatPattern by patternGroup.pattern(
        "from",
        "§aMoved §r§e(?<amount>\\d+) (?<item>.+)§r§a from your Sacks to your inventory."
    )
    private val missingChatPattern by patternGroup.pattern(
        "missing",
        "§cYou have no (?<item>.+) in your Sacks!"
    )

    fun getFromSack(item: NEUInternalName, amount: Int) = getFromSack(item.makePrimitiveStack(amount))

    fun getFromSack(item: PrimitiveItemStack) = getFromSack(listOf(item))

    fun getFromSack(items: List<PrimitiveItemStack>) = addToQueue(items)

    fun getFromChatMessageSackItems(
        item: PrimitiveItemStack,
        text: String = "§lCLICK HERE§r§e to grab §ax${item.amount} §9${item.itemName}§e from sacks!"
    ) =
        ChatUtils.clickableChat(text, "${commands.first()} ${item.internalName.asString()} ${item.amount}")

    fun getFromSlotClickedSackItems(items: List<PrimitiveItemStack>, slotIndex: Int) = addToInventory(items, slotIndex)

    fun Slot.getFromSackWhenClicked(items: List<PrimitiveItemStack>) = getFromSlotClickedSackItems(items, slotIndex)

    private val minimumDelay = 1.65.seconds

    private val queue: Deque<PrimitiveItemStack> = LinkedList()
    private val inventoryMap = mutableMapOf<Int, List<PrimitiveItemStack>>()

    private var lastTimeOfCommand = SimpleTimeMark.farPast()

    private var lastItemStack: PrimitiveItemStack? = null

    var sackList = emptyList<NEUInternalName>()
        private set

    private fun addToQueue(items: List<PrimitiveItemStack>) = queue.addAll(items)

    private fun addToInventory(items: List<PrimitiveItemStack>, slotId: Int) = inventoryMap.put(slotId, items)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (queue.isNotEmpty() && lastTimeOfCommand.passedSince() >= minimumDelay) {
            val item = queue.poll()
            ChatUtils.sendCommandToServer("gfs ${item.internalName.asString()} ${item.amount}")
            lastTimeOfCommand = ChatUtils.getTimeWhenNewlyQueuedMessageGetsExecuted()
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
            tip.addAll(list.map { "§ex" + it.amount.toString() + " " + it.internalName.asString() })
        }
    }

    @SubscribeEvent
    fun onMessageToServer(event: MessageSendToServerEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.queuedGFS && !config.bazaarGFS) return
        if (!event.isCommand(commandsWithSlash)) return
        queuedHandler(event)
        bazaarHandler(event)
    }

    private fun queuedHandler(event: MessageSendToServerEvent) {
        if (!config.queuedGFS) return
        if (event.originatingModContainer?.modId == "skyhanni") return

        val (result, stack) = commandValidator(event.splitMessage.drop(1))

        when (result) {
            CommandResult.VALID -> getFromSack(stack ?: return)
            CommandResult.WRONG_ARGUMENT -> ChatUtils.userError("Missing arguments! Usage: /getfromsacks <name/id> <amount>")
            CommandResult.WRONG_IDENTIFIER -> ChatUtils.userError("Couldn't find an item with this name or identifier!")
            CommandResult.WRONG_AMOUNT -> ChatUtils.userError("Invalid amount!")
        }
        event.isCanceled = true
    }

    private fun bazaarHandler(event: MessageSendToServerEvent) {
        if (event.isCanceled) return
        if (!config.bazaarGFS || LorenzUtils.noTradeMode) return
        lastItemStack = commandValidator(event.splitMessage.drop(1)).second
    }

    private fun bazaarMessage(item: String, amount: Int, isRemaining: Boolean = false) = ChatUtils.clickableChat(
        "§lCLICK §r§eto get the ${if (isRemaining) "remaining " else ""}§ax${amount} §9$item §efrom bazaar",
        "bz ${item.removeColor()}"
    )

    private fun commandValidator(args: List<String>): Pair<CommandResult, PrimitiveItemStack?> {
        if (args.size != 2) {
            return CommandResult.WRONG_ARGUMENT to null
        }

        val item = args[0].asInternalName()

        if (!sackList.contains(item)) {
            return CommandResult.WRONG_IDENTIFIER to null
        }

        val amountString = args[1]

        if (!amountString.isInt()) {
            return CommandResult.WRONG_AMOUNT to null
        }

        return CommandResult.VALID to item.makePrimitiveStack(amountString.toInt())
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.bazaarGFS || LorenzUtils.noTradeMode) return
        val stack = lastItemStack ?: return
        val message = event.message
        fromSacksChatPattern.matchMatcher(message) {
            val diff = stack.amount - group("amount").toInt()
            if (diff <= 0) return
            bazaarMessage(stack.itemName, diff, true)
            lastItemStack = null
            return
        }
        missingChatPattern.matchMatcher(message) {
            bazaarMessage(stack.itemName, stack.amount)
            lastItemStack = null
            return
        }
    }

    private enum class CommandResult {
        VALID,
        WRONG_ARGUMENT,
        WRONG_IDENTIFIER,
        WRONG_AMOUNT
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackList = event.getConstant<SacksJson>("Sacks").sackList.map { it.replace(" ", "_").asInternalName() }
    }
}
