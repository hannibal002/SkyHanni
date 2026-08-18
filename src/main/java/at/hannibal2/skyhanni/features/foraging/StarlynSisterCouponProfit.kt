package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.readItemAmount
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sublistAfter
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import kotlin.math.round

@SkyHanniModule
object StarlynSisterCouponProfit {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest
    private val sisterTypeMap = StarlynSisterType.entries.associateBy { it.inventoryName }

    private var display = emptyList<Renderable>()
    private var currentDisplayMode = DisplayMode.PER_COUPON
    private var currentSisterType: StarlynSisterType? = null

    private var cachedItemData: List<ItemProfitData> = emptyList()

    private data class ItemProfitData(
        val slot: Int,
        val internalName: NeuInternalName,
        val itemName: String,
        val price: Double,
        val totalCost: Double,
        val requiredItems: Map<NeuInternalName, Int>,
        val couponAmount: Int,
        val profit: Double,
        val profitPerCoupon: Double,
        val isCouponPrizeItem: Boolean,
    )

    private val starlynInventory = InventoryDetector(
        checkInventoryName = sisterTypeMap.keys::contains,
        onOpenInventory = { event ->
            if (config.starlynCouponProfitEnabled) {
                sisterTypeMap[event.inventoryName]?.let { sister ->
                    currentSisterType = sister
                    cachedItemData = buildItemData(event, sister)
                    updateDisplay()
                }
            }
        },
        onCloseInventory = {
            currentSisterType = null
            cachedItemData = emptyList()
            display = emptyList()
        },
    )

    enum class DisplayMode(val display: String) {
        PER_COUPON("Coupon"),
        PER_SELL("Sell"),
        ;

        override fun toString(): String = display
    }

    private fun updateDisplay() {
        display = buildList {
            addString("§eProfit per ${currentDisplayMode.display}")
            addRenderableButton<DisplayMode>(
                "§7Display Mode",
                current = currentDisplayMode,
                onChange = {
                    currentDisplayMode = it
                    updateDisplay()
                },
            )
            add(RenderableUtils.fillTable(buildTableEntries(), padding = 3, itemScale = 0.7))
        }
    }

    private fun buildTableEntries(): List<DisplayTableEntry> = cachedItemData.mapNotNull { data ->
        if (currentDisplayMode == DisplayMode.PER_COUPON && data.isCouponPrizeItem) return@mapNotNull null
        toDisplayEntry(data)
    }

    private fun toDisplayEntry(data: ItemProfitData): DisplayTableEntry {
        val displayedProfit = if (currentDisplayMode == DisplayMode.PER_COUPON) data.profitPerCoupon else data.profit
        return DisplayTableEntry(
            data.itemName.asComponent(),
            "§6${displayedProfit.shortFormat()}".asComponent(),
            displayedProfit,
            data.internalName,
            buildHoverText(data).mapToComponents(),
            highlightsOnHoverSlots = listOf(data.slot),
        )
    }
    private fun buildItemData(event: InventoryFullyOpenedEvent, sister: StarlynSisterType): List<ItemProfitData> =
        event.inventoryItems.mapNotNull { (slot, item) ->
            try {
                readItem(slot, item, sister)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error while reading item '${item.repoItemName}'",
                    "item" to item,
                    "name" to item.repoItemName,
                    "inventory name" to event.inventoryName,
                )
                null
            }
        }

    private fun readItem(slot: Int, item: SafeItemStack, sister: StarlynSisterType): ItemProfitData? {
        if (!isValidSlotNumber(slot)) return null

        val internalName = item.getInternalNameOrNull()
        //needed for attribute shards to work correctly
            ?: ItemResolutionQuery.attributeNameToInternalName(item.hoverName.string)
                ?.let { NeuInternalName.fromItemNameOrInternalName(it) }
            ?: return null
        val itemName = internalName.repoItemName

        val requiredItems = getRequiredItems(item)
        val price = internalName.getPrice()
        val totalCost = requiredItems.entries.sumOf { (name, amount) -> (name.getPriceOrNull() ?: 0.0) * amount }
        val couponAmount = requiredItems[sister.couponName] ?: 0

        val rawProfit = price - totalCost
        val profit = round(rawProfit * 100.0) / 100.0
        val profitPerCoupon = if (couponAmount > 0) (profit / couponAmount) else 0.0

        return ItemProfitData(
            slot = slot,
            internalName = internalName,
            itemName = itemName,
            price = price,
            totalCost = totalCost,
            requiredItems = requiredItems,
            couponAmount = couponAmount,
            profit = profit,
            profitPerCoupon = profitPerCoupon,
            isCouponPrizeItem = requiredItems.containsKey(sister.prizeName),
        )
    }


    private fun buildHoverText(data: ItemProfitData): List<Any> = buildList {
        add(data.itemName)
        add("")
        add("§7Sell price: §6${data.price.shortFormat()}")
        add("§7Total cost: §6${data.totalCost.shortFormat()}")
        for ((requiredName, amount) in data.requiredItems) {
            add(requiredName.getPriceName(amount))
        }
        add("")
        add("§7Profit per sell: §6${data.profit.shortFormat()}")
        if (data.couponAmount > 0) {
            add("§7Profit per coupon: §6${data.profitPerCoupon.shortFormat()}")
        }
    }

    private fun getRequiredItems(item: SafeItemStack): Map<NeuInternalName, Int> {
        val lore = item.getLore()
        return lore
            .sublistAfter({ it == "§7Cost" }, skip = 1, amount = lore.size)
            .takeWhile { it.isNotEmpty() }
            .mapNotNull { line ->
                val rawItemName = line.replace("§8 ", " §8")
                readItemAmount(rawItemName)?.let { (name, amount) ->
                    NeuInternalName.fromItemName(name) to amount
                } ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Error getting required item cost for item '${item.repoItemName}'",
                        "Could not parse required item cost from lore ",
                        "rawItemName" to rawItemName,
                        "name" to item.hoverName.formattedTextCompatLeadingWhiteLessResets(),
                        "lore" to lore,
                    )
                    null
                }
            }.toMap()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender() {
        if (!config.starlynCouponProfitEnabled || !starlynInventory.isInside()) return

        display.let {
            config.starlynCouponProfitPos.renderRenderables(it, posLabel = "Starlyn Sister's Shop Profit")
        }
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot !in 9..44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(139, "foraging.starlynContest.agathaCouponProfitEnabled", "foraging.starlynContest.starlynCouponProfitEnabled")
        event.move(139, "foraging.starlynContest.agathaCouponProfitPos", "foraging.starlynContest.starlynCouponProfitPos")
    }
}
