package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsDryStreakDisplay.experimentInventoriesPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ExperimentsProfitTracker {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.experimentsProfitTracker

    private val tracker = SkyHanniItemTracker(
        "Experiments Profit Tracker",
        { Data() },
        { it.experimentsProfitTracker },
    ) { drawDisplay(it) }

    private var inExperiment = false
    private var inExperimentationTable = false
    private var lastExperimentTime = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("enchanting.experiments.profittracker")
    val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^ \\+(?<reward>.*)\$",
    )
    val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp",
    )

    class Data : ItemTrackerData() {
        override fun resetItems() {
            experimentsDone = 0L
            xpGained = 0L
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / experimentsDone
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = listOf<String>()

        @Expose
        var experimentsDone = 0L

        @Expose
        var xpGained = 0L
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (lastExperimentTime.passedSince() > 3.seconds) return

        experimentsDropPattern.matchMatcher(event.message.removeColor()) {
            enchantingExpPattern.matchMatcher(group("reward")) {
                tracker.modify {
                    it.xpGained += group("amount").substringBefore(",").toInt() * 1000
                }
                if (config.hideMessage) event.blockedReason = "experiment_drop"
                return
            }
            val internalName = NEUInternalName.fromItemNameOrNull(group("reward")) ?: return

            tracker.addItem(internalName, 1)

            if (config.hideMessage) event.blockedReason = "experiment_drop"
            return
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (experimentInventoriesPattern.matches(event.inventoryName)) inExperimentationTable = true
        if (InventoryUtils.getCurrentExperiment() != null) inExperiment = true
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return
        if (inExperiment) {
            lastExperimentTime = SimpleTimeMark.now()
            tracker.modify {
                it.experimentsDone++
            }
            inExperiment = false
        }
        if (inExperimentationTable) inExperimentationTable = false
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lExperiments Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        val experimentsDone = data.experimentsDone
        addAsSingletonList("")
        addAsSingletonList("§eExperiments Done: §a${experimentsDone.addSeparators()}",)
        addAsSingletonList(tracker.addTotalProfit(profit, data.experimentsDone, "experiment"))
        addAsSingletonList("§eTotal Enchanting Exp: §b${data.xpGained.shortFormat()}")

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (lastExperimentTime.passedSince() > config.timeDisplayed.seconds && !inExperimentationTable) return

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.PRIVATE_ISLAND) {
            tracker.firstUpdate()
        }
    }

    fun resetCommand() {
        tracker.resetCommand()
    }

    fun isEnabled() = config.enabled && LorenzUtils.inSkyBlock
}
