package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.MinionCloseEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InfernoMinionProfitTracker {

    private val config get() = SkyHanniMod.feature.misc.minions.infernoMinionProfitTracker

    private val infernoMinionInventory = InventoryDetector { name ->
        InfernoMinionFeatures.infernoMinionTitlePattern.matches(name)
    }

    private var isInfernoMinion = false
    private var lastFuelItem: NeuInternalName? = null
    private var lastCollectionTime = SimpleTimeMark.farPast()
    private var itemsCollected = false

    private val tracker = SkyHanniItemTracker(
        "Inferno Minion Profit Tracker",
        { Data() },
        { it.infernoMinionProfitTracker },
        trackerConfig = { config.perTrackerConfig },
    ) { drawDisplay(it) }

    data class Data(
        @Expose var totalFuelCost: Double = 0.0,
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {

        override fun getDescription(timesGained: Long): List<String> {
            val totalItems = items.values.sumOf { it.timesGained }
            val percentage = if (totalItems > 0) timesGained.toDouble() / totalItems else 0.0
            val dropRate = "%.1f%%".format(percentage * 100)
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Drop chance: §c$dropRate",
            )
        }

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = listOf<String>()
    }

    init {
        RenderDisplayHelper(
            inventory = infernoMinionInventory,
            outsideInventory = true,
            condition = { config.enabled && SkyBlockUtils.inSkyBlock && (infernoMinionInventory.isInside() || isRecentCollection()) },
            onRender = { tracker.renderDisplay(config.position) },
        )
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lInferno Minion Profit Tracker")

        var profit = tracker.drawItems(data, { true }, this)
        profit = addFuelCost(data, profit)

        val totalCollections = data.items.values.sumOf { it.timesGained }
        add(
            Renderable.hoverTips(
                "§7Total collections: §e${totalCollections.addSeparators()}",
                listOf("§7You've collected from Inferno Minions §e${totalCollections.addSeparators()} §7times."),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()
        addAll(tracker.addTotalProfit(profit, totalCollections, "collection", duration, "Collections"))

        tracker.addPriceFromButton(this)
    }

    private fun MutableList<Searchable>.addFuelCost(data: Data, profit: Double): Double {
        val fuelCost = data.totalFuelCost
        if (fuelCost <= 0) return profit
        add(
            Renderable.hoverTips(
                "§7Fuel cost: §c-${fuelCost.shortFormat()}",
                listOf(
                    "§7Total spent on fuel items.",
                    "§7This is subtracted from your profit.",
                ),
            ).toSearchable("Fuel Cost"),
        )
        return profit - fuelCost
    }

    @HandleEvent
    fun onMinionOpen(event: MinionOpenEvent) {
        val firstOpen = !isInfernoMinion
        isInfernoMinion = InfernoMinionFeatures.infernoMinionTitlePattern.matches(event.inventoryName)
        if (!isInfernoMinion) return
        if (firstOpen) {
            lastFuelItem = getFuelFromInventory(event.inventoryItems)
        }
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isInfernoMinion) return
        if (!config.enabled) return
        val newFuel = getFuelFromInventory(event.inventoryItems)
        if (newFuel != null && newFuel != lastFuelItem) {
            tracker.modify { it.totalFuelCost += newFuel.getPrice() }
        }
        lastFuelItem = newFuel
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemAddInInventory(event: ItemAddInInventoryEvent) {
        if (!isInfernoMinion) return
        if (!config.enabled) return
        tracker.addItem(event.internalName, event.amount, command = false)
        itemsCollected = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSackChange(event: SackChangeEvent) {
        if (!config.enabled) return
        if (lastCollectionTime.passedSince() > 5.seconds) return
        for (sackChange in event.sackChanges) {
            if (sackChange.delta > 0) {
                tracker.addItem(sackChange.internalName, sackChange.delta, command = false)
            }
        }
    }

    @HandleEvent
    fun onMinionClose(event: MinionCloseEvent) {
        if (!isInfernoMinion) return
        if (!config.enabled) return
        lastCollectionTime = SimpleTimeMark.now()
        lastFuelItem = null
        isInfernoMinion = false
        itemsCollected = false
    }

    private fun getFuelFromInventory(inventoryItems: Map<Int, ItemStack>): NeuInternalName? {
        val fuelStack = inventoryItems[19] ?: return null
        val name = fuelStack.getInternalNameOrNull() ?: return null
        return if (name in InfernoMinionFeatures.fuelItemIds) name else null
    }

    private fun isRecentCollection() = config.showAfterCollection && lastCollectionTime.passedSince() < 10.seconds

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetinfernominiontracker") {
            description = "Resets the Inferno Minion Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
