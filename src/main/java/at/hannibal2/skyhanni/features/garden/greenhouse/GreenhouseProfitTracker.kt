package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.addCollectionCounter
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GreenhouseProfitTracker {
    private val PEST_SACK_BATCH_WINDOW = 6.seconds
    private val config get() = SkyHanniMod.feature.garden.greenhouse.profitTracker
    private var wasTracking = false
    private var lastPestKill = SimpleTimeMark.farPast()

    private val tracker = SkyHanniItemTracker(
        "Greenhouse Profit Tracker",
        ::Data,
        { it.garden.greenhouse.getOrCreateProfitTracker() },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true,
    ) { drawDisplay(it) }

    data class Data(
        @Expose var pickups: Long = 0,
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> = listOf(
            "§7Picked up on a Greenhouse plot §e${timesGained.addSeparators()} §7times.",
        )

        override fun getCoinName(item: TrackedItem) = "§6Greenhouse Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> = listOf(
            "§7Coins gained while on a Greenhouse plot.",
        )
    }

    init {
        tracker.initRenderer(
            { config.position },
            onlyOnIsland = IslandType.GARDEN,
        ) { isEnabled() }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return

        if (event.source == ItemAddManager.Source.SACKS) {
            DelayedRun.runNextTickEnd { processItemAdd(event) }
            return
        }
        processItemAdd(event)
    }

    private fun processItemAdd(event: ItemAddEvent) {
        if (event.source != ItemAddManager.Source.COMMAND && isPestDrop(event)) return

        with(tracker) { event.addItemFromEvent() }
        if (event.source == ItemAddManager.Source.COMMAND) return

        tracker.modify { it.pickups++ }
        addToGreenhouseCollection(event)
    }

    private fun addToGreenhouseCollection(event: ItemAddEvent) {
        val primitiveStack = NeuItems.getPrimitiveMultiplier(event.internalName)
        val crop = CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor) ?: return
        crop.addCollectionCounter(CropCollectionType.GREENHOUSE, primitiveStack.amount.toLong() * event.amount)
    }

    private fun isPestDrop(event: ItemAddEvent): Boolean {
        val recentlyUsedPestTool = PestApi.hasVacuumOrLassoInHand() ||
            PestApi.lastTimeVacuumHeld.passedSince() < PEST_SACK_BATCH_WINDOW ||
            PestApi.lastTimeLassoHeld.passedSince() < PEST_SACK_BATCH_WINDOW
        return PestProfitTracker.isPestDropItem(event.internalName) &&
            (recentlyUsedPestTool || lastPestKill.passedSince() < 3.seconds)
    }

    @HandleEvent(PestKillEvent::class)
    private fun onPestKill() {
        lastPestKill = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(10)) return
        val tracking = isEnabled()
        if (tracking == wasTracking) return
        wasTracking = tracking
        if (tracking) tracker.startSessionUptime() else tracker.pauseSessionUptime()
    }

    @HandleEvent
    private fun onWorldChange() {
        wasTracking = false
        lastPestKill = SimpleTimeMark.farPast()
        tracker.pauseSessionUptime()
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§6§lGreenhouse Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)
        addAll(tracker.addTotalProfit(profit, data.pickups, "pickup", data.getTotalUptime(), "Pickups"))
        tracker.addPriceFromButton(this)
    }

    private fun isEnabled() = config.enabled && GreenhouseUtils.isInGreenhouse()

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetgreenhousetracker") {
            aliases = listOf("shresetght")
            description = "Resets the Greenhouse Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
