package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.foraging.StarlynSisterDetector.createStarlynDetector
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import kotlin.math.round

@SkyHanniModule
object StarlynSisterCouponProfit {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest

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
        val requiredItems: List<LoreCostUtils.LoreCostEntry>,
        val couponAmount: Long,
        val profit: Double,
        val profitPerCoupon: Double,
        val isCouponPrizeItem: Boolean,
    )

    private val starlynInventory = createStarlynDetector(
        isEnabled = { config.starlynCouponProfitEnabled },
        setSisterType = { currentSisterType = it },
        onOpen = { event, sister ->
            cachedItemData = buildItemData(event, sister)
            updateDisplay()
        },
        onClose = {
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

    private fun buildTableEntries(): List<DisplayTableEntry> = cachedItemData.mapNotNull { data ->
        if (currentDisplayMode == DisplayMode.PER_COUPON && data.isCouponPrizeItem) return@mapNotNull null
        toDisplayEntry(data)
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

    private fun readItem(slot: Int, item: SafeItemStack, sister: StarlynSisterType): ItemProfitData? {
        if (!isValidSlotNumber(slot)) return null

        val hoverName = item.hoverName.string
        val fixedDisplayName = hoverName.replace("[Lvl 100]", "[Lvl {LVL}]")

        val internalName = item.run {
            ItemResolutionQuery.attributeNameToInternalName(fixedDisplayName)
                ?.let { NeuInternalName.fromItemNameOrInternalName(it) }
                ?: if (getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK) getInternalNameOrNull()
                else NeuInternalName.fromItemNameOrNull(fixedDisplayName)

        } ?: return null

        //Avoids showing upgrades in the table
        if (internalName.isKnownItem().not()) return null

        val itemName = internalName.repoItemName

        var totalCost = 0.0
        var couponAmount = 0L
        var isCouponPrizeItem = false

        val requiredItems = item.readLoreCosts()
        requiredItems.forEach { (name, amount) ->
            totalCost += (name.getPriceOrNull() ?: 0.0) * amount
            if (name == sister.couponName) couponAmount = amount
            if (name == sister.prizeName) isCouponPrizeItem = true
        }

        val price = internalName.getPrice()
        val profit = round((price - totalCost) * 100.0) / 100.0
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
            isCouponPrizeItem = isCouponPrizeItem,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.starlynCouponProfitEnabled || !starlynInventory.isInside()) return

        config.starlynCouponProfitPos.renderRenderables(display, posLabel = "Starlyn Sister's Shop Profit")
    }

    private fun isValidSlotNumber(slot: Int): Boolean = slot in 9..44 && slot % 9 !in setOf(0, 8)

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(139, "foraging.starlynContest.agathaCouponProfitEnabled", "foraging.starlynContest.starlynCouponProfitEnabled")
        event.move(139, "foraging.starlynContest.agathaCouponProfitPos", "foraging.starlynContest.starlynCouponProfitPos")
    }
}
