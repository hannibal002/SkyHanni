package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingAPI
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addButton
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

typealias CategoryName = String

object FishingProfitTracker {
    val config get() = SkyHanniMod.feature.fishing.fishingProfitTracker

    private val coinsChatPattern = ".* CATCH! §r§bYou found §r§6(?<coins>.*) Coins§r§b\\.".toPattern()

    private val tracker = SkyHanniItemTracker(
        "Fishing Profit Tracker",
        { Data() },
        { it.fishing.fishingProfitTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {
        override fun resetItems() {
            totalCatchAmount = 0
        }

        override fun getDescription(timesCaught: Long): List<String> {
            val percentage = timesCaught.toDouble() / totalCatchAmount
            val catchRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            return listOf(
                "§7Caught §e${timesCaught.addSeparators()} §7times.",
                "§7Your catch rate: §c$catchRate"
            )
        }

        override fun getCoinFormat(item: TrackedItem, numberColor: String): Pair<String, List<String>> {
            val mobKillCoinsFormat = NumberUtil.format(item.totalAmount)
            val gained = item.timesGained
            val text = "$numberColor${gained}x §6Fished Coins§7: §6$mobKillCoinsFormat"
            val lore = listOf(
                "§7Killing mobs gives you coins (more with scavenger)",
                "§7You got §e$mobKillCoinsFormat §7coins in total this way"
            )
            return text to lore
        }

        override fun getCustomPricePer(internalName: NEUInternalName): Double {
            // TODO find better way to tell if the item is a trophy
            val neuInternalNames = itemCategories["Trophy Fish"]!!

            return if (internalName in neuInternalNames) {
                SkyHanniTracker.getPricePer(MAGMA_FISH) * FishingAPI.getFilletPerTrophy(internalName)
            } else super.getCustomPricePer(internalName)
        }

        @Expose
        var totalCatchAmount = 0L
    }

    private val ItemTrackerData.TrackedItem.timesCaught get() = timesGained

    private val MAGMA_FISH by lazy { "MAGMA_FISH".asInternalName() }

    private val nameAll: CategoryName = "All"
    private var currentCategory: CategoryName = nameAll

    private fun getCurrentCategories(data: Data): Map<CategoryName, Int> {
        val map = mutableMapOf<CategoryName, Int>()
        map[nameAll] = data.items.size
        for ((name, items) in itemCategories) {
            val amount = items.count { it in data.items }
            if (amount > 0) {
                map[name] = amount
            }
        }

        return map
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lFishing Profit Tracker")
        val amounts = getCurrentCategories(data)
        val list = amounts.keys.toList()
        if (currentCategory !in list) {
            currentCategory = nameAll
        }
        addButton(
            prefix = "§7Category: ",
            getName = currentCategory + " §7(" + amounts[currentCategory] + ")",
            onChange = {
                val id = list.indexOf(currentCategory)
                currentCategory = list[(id + 1) % list.size]
                tracker.update()
            }
        )

        val filter: (NEUInternalName) -> Boolean = if (currentCategory == nameAll) {
            { true }
        } else {
            val items = itemCategories[currentCategory]!!
            { it in items }
        }

        var profit = 0.0
        val map = mutableMapOf<Renderable, Long>()
        for ((internalName, itemProfit) in data.items) {
            if (!filter(internalName)) continue

            val price = data.drawItem(tracker, itemProfit, internalName, map)
            profit += price
        }

        for (text in map.sortedDesc().keys) {
            addAsSingletonList(text)
        }

        val fishedCount = data.totalCatchAmount
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Times fished: §e${fishedCount.addSeparators()}",
                listOf("§7You catched §e${fishedCount.addSeparators()} §7times something.")
            )
        )

        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitPerCatch = profit / data.totalCatchAmount
        val profitPerCatchFormat = NumberUtil.format(profitPerCatch)

        val text = "§eTotal Profit: $profitPrefix$profitFormat"
        addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per catch: $profitPrefix$profitPerCatchFormat")))

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return
        DelayedRun.runDelayed(500.milliseconds) {
            maybeAddItem(event.internalName, event.amount)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        coinsChatPattern.matchMatcher(event.message) {
            val coins = group("coins").formatNumber()
            tracker.addCoins(coins.toInt())
            addCatch()
        }
    }

    private fun addCatch() {
        tracker.modify {
            it.totalCatchAmount++
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!FishingAPI.hasFishingRodInHand()) return
        if (FishingProfitPlayerMoving.isMoving && config.hideMoving) return

        tracker.renderDisplay(config.position)
    }

    private fun maybeAddItem(internalName: NEUInternalName, amount: Int) {
        if (FishingAPI.lastActiveFishingTime.passedSince() > 10.minutes) return

        if (!isAllowedItem(internalName)) {
            LorenzUtils.debug("Ignored non-fishing item pickup: $internalName'")
            return
        }

        tracker.addItem(internalName, amount)
        addCatch()
    }

    private val itemCategories get() = FishingTrackerCategoryManager.itemCategories

    private fun isAllowedItem(internalName: NEUInternalName) = itemCategories.any { internalName in it.value }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        tracker.firstUpdate()
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetfishingtracker")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
