package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.mining.FossilExcavationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ExcavatorProfitTracker {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.profitTracker

    private val tracker = SkyHanniItemTracker(
        "Fossil Excavation Profit Tracker",
        { Data() },
        { it.mining.fossilExcavatorProfitTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {
        override fun resetItems() {
            timesExcavated = 0
            glacitePowderGained = 0
            fossilDustGained = 0
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

        @Expose
        var glacitePowderGained = 0L

        @Expose
        var fossilDustGained = 0L
    }

    private val scrapItem get() = FossilExcavatorAPI.scrapItem

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

        profit = addScrap(timesExcavated, profit)
        if (config.showFossilDust) {
            profit = addFossilDust(data.fossilDustGained, profit)
        }
        if (config.trackGlacitePowder) {
            addGlacitePowder(data)
        }

        addAsSingletonList(tracker.addTotalProfit(profit, data.timesExcavated, "excavation"))

        tracker.addPriceFromButton(this)
    }

    private fun MutableList<List<Any>>.addFossilDust(
        fossilDustGained: Long,
        profit: Double,
    ): Double {
        if (fossilDustGained <= 0) return profit
        // TODO use same price source as profit tracker
        val pricePer = scrapItem.getPrice() / 500
        val fossilDustPrice = pricePer * fossilDustGained
        addAsSingletonList(
            Renderable.hoverTips(
                "§7${fossilDustGained.shortFormat()}x §fFossil Dust§7: §6${fossilDustPrice.shortFormat()}",
                listOf(
                    "§7You gained §6${fossilDustPrice.shortFormat()} coins §7in total",
                    "§7for all §e$fossilDustGained §fFossil Dust",
                    "§7you have collected.",
                    "",
                    "§7Price Per Fossil Dust: §6${pricePer.shortFormat()}"
                )
            )
        )
        return profit + fossilDustPrice
    }

    private fun MutableList<List<Any>>.addGlacitePowder(data: Data) {
        val glacitePowderGained = data.glacitePowderGained
        if (glacitePowderGained <= 0) return
        addAsSingletonList(
            Renderable.hoverTips(
                "§bGlacite Powder§7: §e${glacitePowderGained.addSeparators()}",
                listOf(
                    "§7No real profit,",
                    "§7but still nice to see! Right?",
                )
            )
        )
    }

    private fun MutableList<List<Any>>.addScrap(
        timesExcavated: Long,
        profit: Double,
    ): Double {
        if (timesExcavated <= 0) return profit
        // TODO use same price source as profit tracker
        val scrapPrice = timesExcavated * scrapItem.getPrice()
        val name = StringUtils.pluralize(timesExcavated.toInt(), scrapItem.itemName)
        addAsSingletonList(
            Renderable.hoverTips(
                "$name §7price: §c-${scrapPrice.shortFormat()}",
                listOf(
                    "§7You paid §c${scrapPrice.shortFormat()} coins §7in total",
                    "§7for all §e$timesExcavated $name",
                    "§7you have used."
                )
            )
        )
        return profit - scrapPrice
    }

    @SubscribeEvent
    fun onFossilExcavation(event: FossilExcavationEvent) {
        if (!isEnabled()) return
        for ((name, amount) in event.loot) {
            addItem(name, amount)
        }
        tracker.modify {
            it.timesExcavated++
        }
    }

    private fun addItem(name: String, amount: Int) {
        if (name == "§bGlacite Powder") {
            if (config.trackGlacitePowder) {
                tracker.modify {
                    it.glacitePowderGained += amount
                }
            }
            return
        }
        if (name == "§fFossil Dust") {
            if (config.showFossilDust) {
                tracker.modify {
                    it.fossilDustGained += amount
                }
            }
            return
        }

        val internalName = NEUInternalName.fromItemNameOrNull(name)
        if (internalName == null) {
            ChatUtils.debug("no price for exavator profit: '$name'")
            return
        }
        // TODO use primitive item stacks in trackers
        tracker.addItem(internalName, amount)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        val inChest = Minecraft.getMinecraft().currentScreen is GuiChest
        if (inChest) {
            // Only show in excavation menu
            if (!FossilExcavatorAPI.inExcavatorMenu) {
                return
            }
        }

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.DWARVEN_MINES) {
            tracker.firstUpdate()
        }
    }

    fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.enabled
        && LorenzUtils.skyBlockArea == "Fossil Research Center"
}
