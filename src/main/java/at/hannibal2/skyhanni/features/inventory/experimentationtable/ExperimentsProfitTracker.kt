package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsDryStreakDisplay.experimentInventoriesPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
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
    private var lastSplashes = mutableListOf<ItemStack>()
    private var lastSplashTime = SimpleTimeMark.farPast()
    private var lastBottlesInInventory = mutableMapOf<NEUInternalName, Int>()
    private var currentBottlesInInventory = mutableMapOf<NEUInternalName, Int>()

    private val patternGroup = RepoPattern.group("enchanting.experiments.profittracker")

    /**
     * REGEX-TEST:  +Smite VII
     * REGEX-TEST:  +42,000 Enchanting Exp
     */
    private val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^ \\+(?<reward>.*)\$",
    )

    /**
     * REGEX-TEST: 131k Enchanting Exp
     * REGEX-TEST: 42,000 Enchanting Exp
     */
    private val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp",
    )

    /**
     * REGEX-TEST: Titanic Experience Bottle
     */
    private val experienceBottlePattern by patternGroup.pattern(
        "xpbottle",
        "(?:Titanic |Grand |\\b)Experience Bottle",
    )

    /**
     * REGEX-TEST: ☕ You renewed the experiment table! (1/3)
     */
    private val experimentRenewPattern by patternGroup.pattern(
        "renew",
        "^☕ You renewed the experiment table! \\((?<current>\\d)/3\\)$",
    )

    class Data : ItemTrackerData() {
        override fun resetItems() {
            experimentsDone = 0L
            xpGained = 0L
            bitCost = 0L
            startCost = 0L
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

        @Expose
        var bitCost = 0L

        @Expose
        var startCost = 0L
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled() || lastExperimentTime.passedSince() > 3.seconds) return

        val message = event.message.removeColor()

        experimentsDropPattern.matchMatcher(message) {
            val reward = group("reward")

            enchantingExpPattern.matchMatcher(reward) {
                tracker.modify {
                    it.xpGained += group("amount").substringBefore(",").toInt() * 1000
                }
                if (config.hideMessage) event.blockedReason = "experiment_drop"
                return
            }

            val internalName = NEUInternalName.fromItemNameOrNull(reward) ?: return
            if (!experienceBottlePattern.matches(group("reward"))) tracker.addItem(internalName, 1)

            if (config.hideMessage) event.blockedReason = "experiment_drop"
            return
        }

        experimentRenewPattern.matchMatcher(message) {
            val increments = mapOf(1 to 150, 2 to 300, 3 to 500)
            tracker.modify {
                it.bitCost += increments.getValue(group("current").toInt())
            }
        }
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (event.clickType == ClickType.RIGHT_CLICK) {
            val item = event.itemInHand ?: return
            if (experienceBottlePattern.matches(item.displayName.removeColor())) {
                lastSplashTime = SimpleTimeMark.now()
                lastSplashes.add(item)
            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (experimentInventoriesPattern.matches(event.inventoryName)) {
            inExperimentationTable = true
            if (lastSplashTime.passedSince() < 30.seconds) {
                var startCostTemp = 0
                val iterator = lastSplashes.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    val internalName = item.getInternalName()
                    val price = internalName.getPrice()
                    val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
                    val maxPrice = npcPrice.coerceAtLeast(price)
                    startCostTemp += maxPrice.round(0).toInt()
                    iterator.remove()
                }
                tracker.modify {
                    it.startCost -= startCostTemp
                }
                lastSplashTime = SimpleTimeMark.farPast()
            }
        }

        val addToTracker = lastExperimentTime.passedSince() <= 3.seconds
        handleExpBottles(addToTracker)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return

        if (InventoryUtils.getCurrentExperiment() != null) {
            lastExperimentTime = SimpleTimeMark.now()
            tracker.modify {
                it.experimentsDone++
            }
        }
        if (inExperimentationTable) {
            lastExperimentTime = SimpleTimeMark.now()
            handleExpBottles(true)
            inExperimentationTable = false
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lExperiments Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this) + data.startCost

        val experimentsDone = data.experimentsDone
        addAsSingletonList("")
        addAsSingletonList("§eExperiments Done: §a${experimentsDone.addSeparators()}")
        val startCostFormat = data.startCost.absoluteValue.shortFormat()
        val bitCostFormat = data.bitCost.shortFormat()
        addAsSingletonList(
            Renderable.hoverTips(
                "§eTotal Cost: §c-$startCostFormat§e/§b-$bitCostFormat",
                listOf(
                    "§7You paid §c$startCostFormat §7coins and", "§b$bitCostFormat §7bits for starting",
                    "§7experiments.",
                ),
            ),
        )
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

    private fun handleExpBottles(addToTracker: Boolean) {
        for (item in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (internalName.asString() !in listOf("EXP_BOTTLE", "GRAND_EXP_BOTTLE", "TITANIC_EXP_BOTTLE")) continue
            currentBottlesInInventory.addOrPut(internalName, item.stackSize)
        }
        for (bottleType in currentBottlesInInventory) {
            val internalName = bottleType.key
            val amount = bottleType.value

            val lastInInv = lastBottlesInInventory.getOrDefault(internalName, 0)
            if (lastInInv >= amount || lastInInv == 0) {
                currentBottlesInInventory[internalName] = 0
                lastBottlesInInventory[internalName] = amount
                continue
            }

            lastBottlesInInventory[internalName] = amount
            currentBottlesInInventory[internalName] = 0
            if (addToTracker) tracker.addItem(internalName, amount - lastInInv)
        }
    }

    fun isEnabled() = config.enabled && LorenzUtils.inSkyBlock
}
