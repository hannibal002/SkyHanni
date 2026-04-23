package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.MinionDropsJson
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.MinionCloseEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.minion.MinionFeatures.MINION_FUEL_SLOT
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.oneDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InfernoMinionProfitTracker {

    private val config get() = SkyHanniMod.feature.misc.minions.infernoProfitTracker

    private val eyedropsItem = "CAPSAICIN_EYEDROPS_NO_CHARGES".toInternalName()

    private val infernoMinionInventory = InventoryDetector(InfernoMinionFeatures.infernoMinionTitlePattern)
    private var fuelDropMap = mapOf<NeuInternalName, Set<NeuInternalName>>()
    private var minionDropMap = mapOf<String, Set<NeuInternalName>>()

    private var isInfernoMinion = false
    private var lastFuelItem: NeuInternalName? = null
    private var lastCollectionTime = SimpleTimeMark.farPast()

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
            val shareOfDrops = if (totalItems > 0) timesGained.toDouble() / totalItems else 0.0
            val formattedShare = shareOfDrops.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Share of drops: §c$formattedShare",
            )
        }

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = emptyList<String>()
    }

    init {
        RenderDisplayHelper(
            inventory = infernoMinionInventory,
            outsideInventory = true,
            onlyOnIsland = IslandType.PRIVATE_ISLAND,
            condition = { config.enabled && (infernoMinionInventory.isInside() || isRecentCollection()) },
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
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<MinionDropsJson>("MinionDrops")
        fuelDropMap = data.fuelDrops
        minionDropMap = data.minions
    }

    @HandleEvent
    fun onMinionOpen(event: MinionOpenEvent) {
        isInfernoMinion = InfernoMinionFeatures.infernoMinionTitlePattern.matches(event.inventoryName)
        if (!isInfernoMinion) return
        lastFuelItem = getFuelFromInventory(event.inventoryItems)
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.enabled) return
        if (!isInfernoMinion) return
        val newFuel = getFuelFromInventory(event.inventoryItems)
        if (newFuel != null && newFuel != lastFuelItem) {
            tracker.modify { it.totalFuelCost += newFuel.getPrice() }
        }
        lastFuelItem = newFuel
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onItemAdd(event: ItemAddEvent) {
        if (!config.enabled) return
        if (lastCollectionTime.passedSince() > 1.minutes) return
        val isKnownDrop = minionDropMap.values.any { event.internalName in it } ||
            fuelDropMap.values.any { event.internalName in it }
        if (!isKnownDrop) return
        tracker.addItem(event.internalName, event.amount, command = false)
    }

    @HandleEvent(MinionCloseEvent::class)
    fun onMinionClose() {
        if (!isInfernoMinion) return
        if (!config.enabled) return
        lastCollectionTime = SimpleTimeMark.now()
        lastFuelItem = null
        isInfernoMinion = false
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return
        if (MinionFeatures.eyedropsRanOutPattern.matches(event.cleanMessage)) {
            tracker.modify { it.totalFuelCost += eyedropsItem.getPrice() }
        }
    }

    private fun getFuelFromInventory(inventoryItems: Map<Int, ItemStack>): NeuInternalName? {
        val fuelStack = inventoryItems[MINION_FUEL_SLOT] ?: return null
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
