package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.events.entity.ItemRemoveInInventoryEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.composterInventory
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.fuelFactors
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.organicMatter
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.organicMatterFactors
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.addPriceFromButton
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.addTotalProfit
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTimedItemTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object ComposterProfitTracker {
    val config get() = GardenApi.config.composters.profitTracker
    val tracker = SkyHanniTimedItemTracker(
        "Composter Profit Tracker",
        { Data() },
        { it.garden.composterProfitTracker },
        drawDisplay = { drawDisplay(it) },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true
    )

    val COMPOST = "Compost".toInternalName()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemAdd(event: ItemAddInInventoryEvent) {
        if (!composterInventory.isInside() || event.internalName != COMPOST) return
        tracker.modify { it.compostGained += event.amount }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemRemoved(event: ItemRemoveInInventoryEvent) {
        if (!(composterInventory.isInside() || InventoryUtils.openInventoryName().startsWith("Insert Crops"))) return
        if (event.internalName !in organicMatter.keys + fuelFactors.keys) return
        tracker.addItem(event.internalName, -event.amount, false)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!composterInventory.isInside()) return
        val item = event.item?.getInternalNameOrNull() ?: return
        if (item !in organicMatter.keys + fuelFactors.keys) return
        tracker.addItem(item, event.item.stackSize, false)
    }

    class TimeData : TimedTrackerData<Data>({ Data() })

    data class Data(
        @Expose var compostGained: Long = 0
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            return listOf(
                "§7You have inserted this §e${timesGained.addSeparators()} times."
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dropped Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val coinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Something has went wrong if you can see this.",
                "§7You somehow gained §6$coinsFormat coins.",
            )
        }
    }

    fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lComposter Profit Tracker")
        val compostAmount = data.compostGained
        val compostProfit = compostAmount * "COMPOST".toInternalName().getPrice()
        addSearchString("§eCompost Earned: ${compostAmount.addSeparators()} ${compostProfit.formatCoin()}")
        addSearchString("§eItems Spent:")
        var profit = tracker.drawItems(data, { true }, this)
        profit += compostProfit

        addSearchString("Total Profit: ${profit.formatCoin()}")

        val duration = data.getTotalUptime()
        val profitPerHourRenderable =
            if (tracker.shouldShowProfitPerHour()) tracker.profitPerHourRenderable(profit, duration) else Renderable.empty()

        add(profitPerHourRenderable.toSearchable())

        addPriceFromButton(this)
    }

    init {
        tracker.initRenderer({ config.position }, composterInventory) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!GardenApi.inGarden()) return false
        if (!config.enabled) return false
        if (config.onlyInInventory && !composterInventory.isInside()) return false

        return true
    }
}
