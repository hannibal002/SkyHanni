package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addButton
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.hours

class ChestValue {

    private val config get() = SkyHanniMod.feature.inventory.chestValueConfig
    private var display = emptyList<List<Any>>()
    private val chestItems = mutableMapOf<NEUInternalName, Item>()
    private val inInventory get() = isValidStorage()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.inDungeons && !config.enableInDungeons) return
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

        init()

        if (chestItems.isEmpty()) return newDisplay

        addList(newDisplay)
        addButton(newDisplay)

        return newDisplay
    }

    private fun addList(newDisplay: MutableList<List<Any>>) {
        val sortedList = sortedList()
        var totalPrice = 0.0
        var rendered = 0
        val amountShowing = if (config.itemToShow > sortedList.size) sortedList.size else config.itemToShow
        newDisplay.addAsSingletonList("§7Estimated Chest Value: §o(Showing $amountShowing of ${sortedList.size} items)")
        for ((index, amount, stack, total, tips) in sortedList) {
            totalPrice += total
            if (rendered >= config.itemToShow) continue
            if (total < config.hideBelow) continue
            val textAmount = " §7x$amount:"
            val width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(textAmount)
            val name = "${stack.displayName.reduceStringLength((config.nameLength - width), ' ')} $textAmount"
            val price = "§b${(total).formatPrice()}"
            val text = if (config.alignedDisplay)
                "$name $price"
            else
                "${stack.displayName} §7x$amount: §b${total.formatPrice()}"
            newDisplay.add(buildList {
                val renderable = Renderable.hoverTips(
                    text,
                    tips,
                    stack = stack,
                    indexes = index
                )
                add(" §7- ")
                if (config.showStacks) add(stack)
                add(renderable)
            })
            rendered++
        }
        newDisplay.addAsSingletonList("§6Total value : §b${totalPrice.formatPrice()}")
    }

    private fun sortedList(): MutableList<Item> {
        return when (config.sortingType) {
            0 -> chestItems.values.sortedByDescending { it.total }
            1 -> chestItems.values.sortedBy { it.total }
            else -> chestItems.values.sortedByDescending { it.total }
        }.toMutableList()
    }

    private fun addButton(newDisplay: MutableList<List<Any>>) {
        newDisplay.addButton("§7Sorted By: ",
            getName = SortType.entries[config.sortingType].longName,
            onChange = {
                config.sortingType = (config.sortingType + 1) % 2
                update()
            })

        newDisplay.addButton("§7Value format: ",
            getName = FormatType.entries[config.formatType].type,
            onChange = {
                config.formatType = (config.formatType + 1) % 2
                update()
            })

        newDisplay.addButton("§7Display Type: ",
            getName = DisplayType.entries[if (config.alignedDisplay) 1 else 0].type,
            onChange = {
                config.alignedDisplay = !config.alignedDisplay
                update()
            })
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
                val internalName = stack.getInternalNameOrNull() ?: continue
                if (internalName.getItemStackOrNull() == null) continue
                val list = mutableListOf<String>()
                val pair = EstimatedItemValue.getEstimatedItemPrice(stack, list)
                var (total, _) = pair
                if (stack.item == Items.enchanted_book)
                    total /= 2
                list.add("§aTotal: §6§l${total.formatPrice()}")
                if (total == 0.0) continue
                val item = chestItems.getOrPut(internalName) {
                    Item(mutableListOf(), 0, stack, 0.0, list)
                }
                item.index.add(i)
                item.amount += stack.stackSize
                item.total += total * stack.stackSize
            }
        }
    }

    private fun Double.formatPrice(): String {
        return when (config.formatType) {
            0 -> if (this > 1_000_000_000) NumberUtil.format(this, true) else NumberUtil.format(this)
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

    enum class DisplayType(val type: String) {
        NORMAL("Normal"),
        COMPACT("Aligned")
    }

    private fun isValidStorage(): Boolean {
        val name = InventoryUtils.openInventoryName().removeColor()
        if (Minecraft.getMinecraft().currentScreen !is GuiChest) return false

        if ((name.contains("Backpack") && name.contains("Slot #") || name.startsWith("Ender Chest (")) &&
            !InventoryUtils.isNeuStorageEnabled.getValue()
        ) {
            return true
        }

        val inMinion = name.contains("Minion") && !name.contains("Recipe") &&
                LorenzUtils.skyBlockIsland == IslandType.PRIVATE_ISLAND
        return name == "Chest" || name == "Large Chest" || inMinion || name == "Personal Vault"
    }

    private fun String.reduceStringLength(targetLength: Int, char: Char): String {
        val mc = Minecraft.getMinecraft()
        val spaceWidth = mc.fontRendererObj.getCharWidth(char)

        var currentString = this
        var currentLength = mc.fontRendererObj.getStringWidth(currentString)

        while (currentLength > targetLength) {
            currentString = currentString.dropLast(1)
            currentLength = mc.fontRendererObj.getStringWidth(currentString)
        }

        val difference = targetLength - currentLength

        if (difference > 0) {
            val numSpacesToAdd = difference / spaceWidth
            val spaces = " ".repeat(numSpacesToAdd)
            return currentString + spaces
        }

        return currentString
    }

    data class Item(
        val index: MutableList<Int>,
        var amount: Int,
        val stack: ItemStack,
        var total: Double,
        val tips: MutableList<String>
    )

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

}