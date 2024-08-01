package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGH
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object CrystalNucleusTracker {
    private val config get() = SkyHanniMod.feature.mining.crystalNucleusTracker

    private val tracker = SkyHanniItemTracker(
        "Crystal Nucleus Tracker",
        { Data() },
        { it.mining.crystalNucleusTracker }) { drawDisplay(it)}

    class Data : ItemTrackerData() {
        override fun resetItems() {
            runsCompleted = 0L
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / runsCompleted
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate."
            )
        }

        // No direct coin drops from nuc runs
        override fun getCoinName(item: TrackedItem) = ""
        override fun getCoinDescription(item: TrackedItem) = mutableListOf<String>()

        @Expose
        var runsCompleted = 0L
    }

    @HandleEvent(priority = HIGH)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        //Todo: Add items to tracker
    }

    private fun addCompletedRun() {
        tracker.modify {
            it.runsCompleted++
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lPest Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        val runsCompleted = data.runsCompleted
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Runs completed: §e${runsCompleted.addSeparators()}",
                listOf("§7You completed §e${runsCompleted.addSeparators()} §7Crystal Nucleus Runs.")
            )
        )
        addAsSingletonList(tracker.addTotalProfit(profit, data.runsCompleted, "run"))

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            tracker.firstUpdate()
        }
    }

    fun resetCommand() {
        tracker.resetCommand()
    }

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.enabled && (CrystalNucleusAPI.isInNucleus() || config.showEverywhere)
}
