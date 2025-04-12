package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.experienceBottlePattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.experimentRenewPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.inventoriesPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import kotlin.math.absoluteValue

@SkyHanniModule
object ExperimentsProfitTracker {

    //  Todo: Move more data handling out of this class and into the ExperimentationTableApi

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.experimentsProfitTracker

    private val tracker = SkyHanniItemTracker(
        "Experiments Profit Tracker",
        { Data() },
        { it.experimentation.experimentsProfitTracker },
    ) { drawDisplay(it) }

    private val lastSplashes = mutableListOf<ItemStack>()
    private var lastSplashTime = SimpleTimeMark.farPast()

    class Data : ItemTrackerData() {
        override fun resetItems() {
            experimentsDone = 0L
            xpGained = 0L
            bitCost = 0L
            startCost = 0L
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / experimentsDone
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
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

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onItemAdd(event: ItemAddEvent) {
        if (isEnabled() && event.source == ItemAddManager.Source.COMMAND) {
            if (config.enabled) {
                tracker.addItem(event.internalName, event.amount, command = true)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        experimentRenewPattern.matchMatcher(event.message.removeColor()) {
            val increments = mapOf(1 to 150, 2 to 300, 3 to 500)
            tracker.modify {
                it.bitCost += increments.getValue(group("current").toInt())
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableTaskCompleted(event: TableTaskCompletedEvent) {
        tracker.modify {
            if (event.type == ExperimentationTableApi.ExperimentationTaskType.SUPERPAIRS) {
                it.experimentsDone++
            }
            it.xpGained += event.enchantingXpGained ?: 0L
        }

    }

    private val allowedSlots = listOf(11, 12, 14, 15)

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() ||
            InventoryUtils.openInventoryName() != "Bottles of Enchanting" ||
            !allowedSlots.contains(event.slotId)
        ) return
        val stack = event.slot?.stack ?: return

        val internalName = stack.getInternalName()
        if (internalName.isExpBottle()) {
            tracker.modify {
                it.startCost -= calculateBottlePrice(internalName)
            }
        }
    }

    private fun NeuInternalName.isExpBottle() = experienceBottlePattern.matches(asString())

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return

        if (inventoriesPattern.matches(event.inventoryName)) {
            var startCostTemp = 0
            for (item in lastSplashes) {
                startCostTemp += calculateBottlePrice(item.getInternalName())
            }
            lastSplashes.clear()
            tracker.modify {
                it.startCost -= startCostTemp
            }
            lastSplashTime = SimpleTimeMark.farPast()
        }
    }

    private fun calculateBottlePrice(internalName: NeuInternalName): Int {
        val price = internalName.getPrice()
        val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
        return npcPrice.coerceAtLeast(price).toInt()
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lExperiments Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this) + data.startCost

        val experimentsDone = data.experimentsDone
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

    init {
        tracker.initRenderer(
            { config.position },
            ExperimentationTableApi.superpairInventory,
        ) { config.enabled && isEnabled() }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.PRIVATE_ISLAND) {
            tracker.firstUpdate()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetexperimentsprofittracker") {
            description = "Resets the Experiments Profit Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    private fun isLocationEnabled(check: Boolean = true) = !check || ExperimentationTableApi.inDistanceToTable(5.0)
    private fun isEnabled(checkDistanceToExperimentationTable: Boolean = true) =
        config.enabled && isLocationEnabled(checkDistanceToExperimentationTable)

}
