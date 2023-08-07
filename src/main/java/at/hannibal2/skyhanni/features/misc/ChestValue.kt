package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class ChestValue {

    private val config get() = SkyHanniMod.feature.inventory.chestValueConfig
    private var display = emptyList<List<Any>>()
    private val chestItems = mutableMapOf<String, Item>()
    private val inInventory get() = InventoryUtils.openInventoryName().isValidStorage()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() == "") return
        if (inInventory) {
            config.position.renderStringsAndItems(
                display,
                extraSpace = -1,
                itemScale = 1.3,
                posLabel = "Estimated Chest Value"
            )
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (inInventory) {
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        chestItems.clear()
        Renderable.list.clear()
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawBackground(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!config.enableHighlight) return
        if (inInventory) {
            for ((_, indexes) in Renderable.list) {
                for (slot in InventoryUtils.getItemsInOpenChest()) {
                    if (indexes.contains(slot.slotIndex)) {
                        slot highlight Color(SpecialColour.specialToChromaRGB(config.highlightColor), true)
                    }
                }
            }
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        var totalPrice = 0.0
        var rendered = 0
        init()

        if (chestItems.isNotEmpty()) {
            val sortedList = when (config.sortingType) {
                0 -> chestItems.values.sortedByDescending { it.total }.toMutableList()
                1 -> chestItems.values.sortedBy { it.total }.toMutableList()
                else -> chestItems.values.sortedByDescending { it.total }.toMutableList()
            }
            val amountShowing = if (config.itemToShow > sortedList.size) sortedList.size else config.itemToShow

            newDisplay.addAsSingletonList("§7Estimated Chest Value: §o(Showing $amountShowing of ${sortedList.size} items)")
            for ((index, amount, stack, total, tips) in sortedList) {
                totalPrice += total
                if (rendered >= config.itemToShow) continue
                if (total < config.hideBelow) continue
                newDisplay.add(buildList {
                    val renderable = Renderable.hoverTips(
                        "${stack.displayName} §7x$amount: §b${(total).formatPrice()}",
                        tips,
                        stack = stack,
                        indexes = index)
                    add(" §7- ")
                    add(stack)
                    add(renderable)
                })
                rendered++
            }

            val sortingType = SortType.entries[config.sortingType].longName
            newDisplay.addAsSingletonList("§7Sorted By: §c$sortingType")
            newDisplay.addSelector(" ", SortType.entries.toTypedArray(),
                getName = { type -> type.shortName },
                isCurrent = { it.ordinal == config.sortingType },
                onChange = {
                    config.sortingType = it.ordinal
                    update()
                })
            newDisplay.addAsSingletonList("§6Total value : §b${totalPrice.formatPrice()}")
            newDisplay.addSelector(" ", FormatType.entries.toTypedArray(),
                getName = { type -> type.type },
                isCurrent = { it.ordinal == config.formatType },
                onChange = {
                    config.formatType = it.ordinal
                    update()
                })
        }
        return newDisplay
    }

    private fun init() {
        if (inInventory) {
            val isMinion = InventoryUtils.openInventoryName().contains(" Minion ")
            val slots = InventoryUtils.getItemsInOpenChest().filter {
                it.hasStack && it.inventory != Minecraft.getMinecraft().thePlayer.inventory && (!isMinion || it.slotNumber % 9 != 1)
            }
            val stacks = buildMap {
                slots.forEach {
                    put(it.slotIndex, it.stack)
                }
            }
            chestItems.clear()
            for ((i, stack) in stacks) {
                val internalName = stack.getInternalName()
                if (internalName == "") continue
                if (NEUItems.getItemStackOrNull(internalName) == null) continue
                val list = mutableListOf<String>()
                val pair = EstimatedItemValue.getEstimatedItemPrice(stack, list)
                var (total, _) = pair
                if (stack.item == Items.enchanted_book)
                    total /= 2
                list.add("§aTotal: §6§l${total.formatPrice()}")
                if (total == 0.0) continue
                if (chestItems.contains(stack.getInternalName())) {
                    val (oldIndex, oldAmount, oldStack, oldTotal, oldTips) = chestItems[stack.getInternalName()]
                        ?: return
                    oldIndex.add(i)
                    chestItems[stack.getInternalName()] = Item(
                        oldIndex,
                        oldAmount + stack.stackSize,
                        oldStack,
                        oldTotal + (total * stack.stackSize),
                        oldTips
                    )
                } else {
                    chestItems[stack.getInternalName()] =
                        Item(mutableListOf(i), stack.stackSize, stack, (total * stack.stackSize), list)
                }
            }
        }
    }

    private fun Double.formatPrice(): String {
        return when (config.formatType) {
            0 -> if (this > 1_000_000_000) NumberUtil.format2(this, true) else NumberUtil.format2(this)
            1 -> this.addSeparators()
            else -> "0"
        }
    }

    enum class SortType(val shortName: String, val longName: String) {
        PRICE_DESC("Price D", "Price Descending"),
        PRICE_ASC("Price A", "Price Ascending")
        ;
    }

    enum class FormatType(val type: String) {
        SHORT("Formatted"),
        LONG("Unformatted")
        ;
    }

    private fun String.isValidStorage() = Minecraft.getMinecraft().currentScreen is GuiChest && ((this == "Chest" ||
        this == "Large Chest") ||
        (contains("Minion") && !contains("Recipe") && LorenzUtils.skyBlockIsland == IslandType.PRIVATE_ISLAND) ||
        this == "Personal Vault")

    data class Item(
        val index: MutableList<Int>,
        val amount: Int,
        val stack: ItemStack,
        val total: Double,
        val tips: MutableList<String>
    )

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}