package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.inventory.ChestValue.ChestItem
import at.hannibal2.hanni.features.inventory.ChestValue.addToList
import at.hannibal2.hanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getCoinItemStack
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatDouble
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderDisplayHelper
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@HanniModule
object TradeValue {
    private val config get() = HanniMod.feature.inventory.trade

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

    /**
     * REGEX-TEST:  §71
     * REGEX-TEST:  §8(1,000)
     */
    private val coinPattern by RepoPattern.pattern(
        "inventory.tradevalue.coins",
        "§(?<type>[87])\\(?(?<number>[^)]*)\\)?",
    )

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
    fun onTick(event: HanniTickEvent) {
        if (!inventory.isInside()) return
        if (!event.isMod(2)) return

        val otherMap = mutableMapOf<Int, ItemStack>()
        val yourMap = mutableMapOf<Int, ItemStack>()
        // Gets total value of trade
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in otherList) {
                otherMap[slot.slotIndex] = slot.stack
            }
            if (slot.slotIndex in yourList) {
                yourMap[slot.slotIndex] = slot.stack
            }
        }
        val (yourCoin, yourTotal) = calculatePrice(yourMap)
        val (otherCoin, otherTotal) = calculatePrice(otherMap)
        if (otherTotal != otherPrevTotal) {
            otherPrevTotal = otherTotal
            val otherItems = ChestValue.createItems(otherMap)
            update(otherItems, TradeSide.OTHER.ordinal, otherCoin)
        }
        if (yourTotal != yourPrevTotal) {
            yourPrevTotal = yourTotal
            val yourItems = ChestValue.createItems(yourMap)
            update(yourItems, TradeSide.YOU.ordinal, yourCoin)
        }
    }

    private fun calculatePrice(items: MutableMap<Int, ItemStack>): Pair<Double?, Double> {
        var coin: Double? = null
        var total = 0.0
        for ((slot, stack) in items.toMap()) {
            if (stack.getLore().contains("§7Lump-sum amount")) {
                if (coin == null) {
                    coinPattern.matchMatcher(stack.getLore().last()) {
                        val number = group("number")
                        coin = number.formatDouble()
                    }
                }
                items.remove(slot)
                continue
            }
            total += (EstimatedItemValueCalculator.calculate(stack, mutableListOf()).first * (stack.stackSize))
        }
        coin?.let {
            total += it
        }
        return coin to total
    }

    // Display trade value breakdown
    private fun update(items: Map<String, ChestItem>, indicator: Int = 0, coin: Double?) {
        val map = items.toMutableMap()
        coin?.let {
            map["Coin: $it"] = ChestItem(
                emptyList<Int>().toMutableList(), 1, getCoinItemStack(coin), it,
                buildList {
                    add("§eCoin value: §6${it.toInt().addSeparators()}")
                },
            )
        }

        if (indicator == 0) {
            yourDisplay = buildList {
                addToList(map.values, "§eTrade Value")
            }
        } else {
            otherDisplay = buildList {
                addToList(map.values, "§eTrade Value")
            }
        }
    }

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && config.enabled

    enum class TradeSide {
        YOU,
        OTHER
    }
}

