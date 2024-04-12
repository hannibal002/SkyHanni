package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.mining.FossilExcavationEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ExcavatorProfitTracker {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.profitTracker

    private val tracker = SkyHanniItemTracker(
        "Fossil Excavation Profit Tracker",
        { Data() },
        { it.mining.fossilExcavatorProfitTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {
        override fun resetItems() {
            timesExcavated = 0
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / timesExcavated
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate."
            )
        }

        override fun getCoinName(item: TrackedItem) = "<no coins>"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            return listOf(
                "<no coins>"
            )
        }

        @Expose
        var timesExcavated = 0L
    }

    private val scrapItem = "SUSPICIOUS_SCRAP".asInternalName()

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lFossil Excavation Profit Tracker")
        var profit = tracker.drawItems(data, { true }, this)

        val timesExcavated = data.timesExcavated
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Times excavated: §e${timesExcavated.addSeparators()}",
                listOf("§7You excavated §e${timesExcavated.addSeparators()} §7times.")
            )
        )

        // TODO use same price source as profit tracker
        val scrapPrice = timesExcavated * scrapItem.getPrice()
        profit -= scrapPrice
        addAsSingletonList(
            Renderable.hoverTips(
                "${scrapItem.itemName}§7: §c${NumberUtil.format(scrapPrice)}",
                listOf(
                    "§7You paid ${NumberUtil.format(scrapPrice)} coins",
                    "§7in total for all §e$timesExcavated §7${scrapItem.itemName}",
                    "§7you have used."
                )
            )
        )

        addAsSingletonList(tracker.addTotalProfit(profit, data.timesExcavated, "excarvation"))

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onFossilExcavation(event: FossilExcavationEvent) {
        if (!isEnabled()) return
        for ((name, amount) in event.loot) {
            println("")
            println("name: '$name'")
            println("amount: $amount")
            val internalName = NEUInternalName.fromItemNameOrNull(name) ?: continue
            val itemStack = internalName.makePrimitiveStack(amount)
            println("itemStack: '$itemStack'")
            tracker.addItem(internalName, amount)
        }
        tracker.modify {
            it.timesExcavated++
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.isCurrentlyFarming()) return
        // TODO add distance check
//         config.showNearvy

        tracker.renderDisplay(config.position)
        tracker.firstUpdate()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.DWARVEN_MINES) {
            tracker.firstUpdate()
        }
    }

    fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.enabled
}
