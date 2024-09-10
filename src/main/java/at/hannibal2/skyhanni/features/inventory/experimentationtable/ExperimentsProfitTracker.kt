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
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableAPI.enchantingExpPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableAPI.experienceBottlePattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableAPI.experimentRenewPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableAPI.experimentsDropPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableAPI.inventoriesPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableEnums.ExperimentMessages
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
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
        { it.experimentationStorage.experimentsProfitTracker },
    ) { drawDisplay(it) }

    private var inExperimentationTable = false
    private var lastExperimentTime = SimpleTimeMark.farPast()
    private var lastSplashes = mutableListOf<ItemStack>()
    private var lastSplashTime = SimpleTimeMark.farPast()
    private var lastBottlesInInventory = mutableMapOf<NEUInternalName, Int>()
    private var currentBottlesInInventory = mutableMapOf<NEUInternalName, Int>()

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
        if (message == "You claimed the Superpairs rewards!" && ExperimentMessages.DONE.isSelected())
            event.blockedReason = "CLAIM_MESSAGE"

        experimentsDropPattern.matchMatcher(message) {
            val reward = group("reward")

            event.blockedReason = when {
                enchantingExpPattern.matches(reward) && ExperimentMessages.EXPERIENCE.isSelected() -> "EXPERIENCE_DROP"
                experienceBottlePattern.matches(reward) && ExperimentMessages.BOTTLES.isSelected() -> "BOTTLE_DROP"
                reward == "Metaphysical Serum" && ExperimentMessages.SERUM.isSelected() -> "SERUM_DROP"
                reward == "Experiment The Fish" && ExperimentMessages.FISH.isSelected() -> "FISH_DROP"
                ExperimentMessages.ENCHANTMENTS.isSelected() -> "ENCHANT_DROP"
                else -> ""
            }

            enchantingExpPattern.matchMatcher(reward) {
                tracker.modify {
                    it.xpGained += group("amount").substringBefore(",").toInt() * 1000
                }
                return
            }

            val internalName = NEUInternalName.fromItemNameOrNull(reward) ?: return
            if (!experienceBottlePattern.matches(group("reward"))) tracker.addItem(internalName, 1, false)
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
        if (inventoriesPattern.matches(event.inventoryName)) {
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

        if (ExperimentationTableAPI.getCurrentExperiment() != null) {
            lastExperimentTime = SimpleTimeMark.now()
            tracker.modify {
                it.experimentsDone++
            }
        }
        if (inExperimentationTable) {
            lastExperimentTime = SimpleTimeMark.now()
            inExperimentationTable = false
        }
        if (lastExperimentTime.passedSince() <= 3.seconds) {
            handleExpBottles(true)
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lExperiments Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this) + data.startCost

        val experimentsDone = data.experimentsDone
        addSearchString("")
        addSearchString("§eExperiments Done: §a${experimentsDone.addSeparators()}")
        val startCostFormat = data.startCost.absoluteValue.shortFormat()
        val bitCostFormat = data.bitCost.shortFormat()
        add(
            Renderable.hoverTips(
                "§eTotal Cost: §c-$startCostFormat§e/§b-$bitCostFormat",
                listOf(
                    "§7You paid §c$startCostFormat §7coins and", "§b$bitCostFormat §7bits for starting",
                    "§7experiments.",
                ),
            ).toSearchable(),
        )
        add(tracker.addTotalProfit(profit, data.experimentsDone, "experiment"))
        addSearchString("§eTotal Enchanting Exp: §b${data.xpGained.shortFormat()}")

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
            if (lastInInv >= amount) {
                currentBottlesInInventory[internalName] = 0
                lastBottlesInInventory[internalName] = amount
                continue
            }
            if (lastInInv == 0) {
                currentBottlesInInventory[internalName] = 0
                lastBottlesInInventory[internalName] = amount
                if (addToTracker && lastExperimentTime.passedSince() <= 3.seconds) tracker.addItem(internalName, amount, false)
                continue
            }

            currentBottlesInInventory[internalName] = 0
            lastBottlesInInventory[internalName] = amount
            if (addToTracker) tracker.addItem(internalName, amount - lastInInv, false)
        }
    }

    fun ExperimentMessages.isSelected() = config.hideMessages.contains(this)

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
