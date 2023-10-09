package at.hannibal2.skyhanni.features.rift.everywhere.motes

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI.motesNpcPrice
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShowMotesNpcSellPrice {
    private val config get() = RiftAPI.config.motes
    private var display = emptyList<List<Any>>()
    private val pattern = ".*(?:§\\w)+You have (?:§\\w)+(?<amount>\\d) Grubber Stacks.*".toPattern()
    private val itemMap = mutableMapOf<String, Pair<MutableList<Int>, Double>>()
    private var inInventory = false
    private val slotList = mutableListOf<Int>()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isInventoryValueEnabled()) return
        if (inInventory) {
            config.inventoryValue.position.renderStringsAndItems(
                display,
                itemScale = 1.3,
                posLabel = "Inventory Motes Value"
            )
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isInventoryValueEnabled()) return
        if (event.isMod(10))
            processItems()
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawSelectedTemplate(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isInventoryValueEnabled()) return
        val name = InventoryUtils.openInventoryName()
        if (!name.contains("Rift Storage")) return
        for ((_, indexes) in Renderable.list) {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                if (indexes.contains(slot.slotIndex)) {
                    slot highlight LorenzColor.GREEN
                }
            }
        }
    }

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!isShowPriceEnabled()) return

        val itemStack = event.itemStack ?: return

        val baseMotes = itemStack.motesNpcPrice() ?: return
        val burgerStacks = config.burgerStacks
        val burgerText = if (burgerStacks > 0) "(${burgerStacks}x≡) " else ""
        val size = itemStack.stackSize
        if (size > 1) {
            event.toolTip.add("§6NPC price: $burgerText§d${baseMotes.addSeparators()} Motes §7($size x §d${(baseMotes / size).addSeparators()} Motes§7)")
        } else {
            event.toolTip.add("§6NPC price: $burgerText§d${baseMotes.addSeparators()} Motes")
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        reset()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        reset()
    }

    private fun reset() {
        if (!isInventoryValueEnabled()) return
        itemMap.clear()
        slotList.clear()
        inInventory = false
        Renderable.list.clear()
    }

    private fun processItems() {
        val inventoryName = InventoryUtils.openInventoryName()
        if (!inventoryName.contains("Rift Storage")) return
        val stacks = InventoryUtils.getItemsInOpenChest().map { it.slotIndex to it.stack }
        itemMap.clear()
        for ((index, stack) in stacks) {
            val itemValue = stack.motesNpcPrice() ?: continue
            val internalName = stack.getInternalName().asString()
            if (itemMap.contains(internalName)) {
                val (oldIndex, oldValue) = itemMap[internalName] ?: return
                oldIndex.add(index)
                itemMap[internalName] = Pair(oldIndex, oldValue + itemValue)
            } else {
                itemMap[internalName] = Pair(mutableListOf(index), itemValue)
            }
        }
        inInventory = true
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!RiftAPI.inRift()) return
        pattern.matchMatcher(event.message) {
            config.burgerStacks = group("amount").toInt()
            chat("§6[SkyHanni] Set your McGrubber's burger stacks to ${group("amount")}.")
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        newDisplay.addAsSingletonList("§7Item Values:")
        val sorted = itemMap.toList().sortedByDescending { it.second.second }.toMap().toMutableMap()

        for ((internalName, pair) in sorted) {
            newDisplay.add(buildList {
                val (index, value) = pair
                add("  §7- ")
                val stack = NEUItems.getItemStack(internalName)
                add(stack)
                val price = value.formatPrice()
                val valuePer = stack.motesNpcPrice() ?: continue
                val tips = buildList {
                    add("§6Item: ${stack.displayName}")
                    add("§6Value per: §d$valuePer Motes")
                    add("§6Total in chest: §d${(value / valuePer).toInt()}")
                    add("")
                    add("§6Total value: §d$price")
                }
                add(Renderable.hoverTips("§6${stack.displayName}: §b$price", tips, indexes = index, stack = stack))
            })
        }
        val total = itemMap.values.fold(0.0) { acc, pair -> acc + pair.second }.formatPrice()
        newDisplay.addAsSingletonList("§7Total price: §b$total")
        val name = FormatType.entries[config.inventoryValue.formatType].type
        newDisplay.addAsSingletonList("§7Price format: §c$name")
        newDisplay.addSelector<FormatType>(
            " ",
            getName = { type -> type.type },
            isCurrent = { it.ordinal == config.inventoryValue.formatType },
            onChange = {
                config.inventoryValue.formatType = it.ordinal
                update()
            }
        )
        return newDisplay
    }

    enum class FormatType(val type: String) {
        SHORT("Short"),
        LONG("Long")
    }

    private fun Double.formatPrice(): String = when (config.inventoryValue.formatType) {
        0 -> NumberUtil.format(this)
        1 -> this.addSeparators()
        else -> "0"
    }

    private fun isShowPriceEnabled() = RiftAPI.inRift() && config.showPrice

    private fun isInventoryValueEnabled() = RiftAPI.inRift() && config.inventoryValue.enabled
}
