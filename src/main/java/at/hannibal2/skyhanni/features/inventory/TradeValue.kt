package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.inventory.ChestValue.ChestItem
import at.hannibal2.skyhanni.features.inventory.ChestValue.addToList
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack

@SkyHanniModule
object TradeValue {
    private val config get() = SkyHanniMod.feature.inventory.trade

    // other person's trade slots
    private val otherList = (5..8).flatMap { x ->
        (0..3).map { y -> x + 9 * y }
    }.toSet()

    private val yourList = (0..3).flatMap { x ->
        (0..3).map { y -> x + 9 * y }
    }.toSet()

    private var otherPrevTotal = 0.0
    private var yourPrevTotal = 0.0
    private var otherDisplay = emptyList<Renderable>()
    private var yourDisplay = emptyList<Renderable>()

    // Detects trade menu thx NEU
    val inventory = InventoryDetector({ onOpen() }) { name -> name.startsWith("You     ") }

    init {
        RenderDisplayHelper(
            inventory,
            condition = { isEnabled() },
        ) {
            config.otherPosition.renderRenderables(otherDisplay, posLabel = "Trade Value")
            config.yourPosition.renderRenderables(yourDisplay, posLabel = "Trade Value")
        }
    }

    private fun onOpen() {
        otherPrevTotal = 0.0
        yourPrevTotal = 0.0
        otherDisplay = emptyList()
        yourDisplay = emptyList()
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!inventory.isInside()) return
        if (!event.isMod(2)) return

        var otherTotal = 0.0
        var yourTotal = 0.0
        val otherMap = mutableMapOf<Int, ItemStack>()
        val yourMap = mutableMapOf<Int, ItemStack>()
        // Gets total value of trade
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val stack = slot.stack
            // Gets value of their trade
            if (slot.slotIndex in otherList) {
                otherMap[slot.slotIndex] = slot.stack
                otherTotal += (EstimatedItemValueCalculator.calculate(stack, mutableListOf()).first * (stack.stackSize))
            }
            // Gets value of your trade
            if (slot.slotIndex in yourList) {
                yourMap[slot.slotIndex] = slot.stack
                yourTotal += (EstimatedItemValueCalculator.calculate(stack, mutableListOf()).first * (stack.stackSize))
            }
        }
        if (otherTotal != otherPrevTotal) {
            otherPrevTotal = otherTotal
            val otherItems = ChestValue.createItems(otherMap)
            update(otherItems, TradeSide.OTHER.ordinal)
        }
        if (yourTotal != yourPrevTotal) {
            yourPrevTotal = yourTotal
            val yourItems = ChestValue.createItems(yourMap)

            update(yourItems, TradeSide.YOU.ordinal)
        }
    }

    // Display trade value breakdown
    private fun update(items: Map<String, ChestItem>, indicator: Int = 0) {
        if (indicator == 0) {
            yourDisplay = buildList {
                addToList(items.values, "§eTrade Value")
            }
        } else {
            otherDisplay = buildList {
                addToList(items.values, "§eTrade Value")
            }
        }
    }

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && config.enabled

    enum class TradeSide {
        YOU,
        OTHER
    }
}
