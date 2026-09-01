package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.effect.EffectApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenUptimeManager
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.tracker.FarmingProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingProfitTracker : SkyHanniBucketedItemTracker<TrackedSource, FarmingProfitTrackerData>(
    "Farming Profit Tracker",
    ::FarmingProfitTrackerData,
    { it.garden.farmingProfitTracker },
    { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.garden.farmingProfitTracker.perTrackerConfig },
    customUptimeControl = true,
) {
    internal val patternGroup = RepoPattern.group("garden.farming.profit.tracker")

    private val stinkyCheesePotion = "POTION_STINKY_CHEESE;1".toInternalName()
    private val harvestHarbingerPotion = "POTION_HARVEST_HARBINGER;5".toInternalName()

    override fun formatItemAmount(amount: Long): String =
        if (amount.absoluteValue >= 1_000_000L) amount.shortFormat() else amount.addSeparators()

    override fun shouldShowExactItemAmount(amount: Long): Boolean = amount.absoluteValue >= 1_000_000L

    internal val config: FarmingProfitTrackerConfig get() = SkyHanniMod.feature.garden.farmingProfitTracker
    internal val trackerDisplayConfig
        get() = if (config.perTrackerConfig.useUniversalConfig) {
            SkyHanniMod.feature.misc.tracker
        } else {
            config.perTrackerConfig.trackerConfig
        }

    internal var lastFarmingActivity = SimpleTimeMark.farPast()
        private set

    private var currentToolHasBountiful = false
    private val trackedPotionEffects = mapOf(
        NonGodPotEffect.DOUCE_PLUIE_DE_STINKY_CHEESE to (TrackedSource.PESTS to stinkyCheesePotion),
        NonGodPotEffect.HARVEST_HARBINGER to (TrackedSource.CROPS to harvestHarbingerPotion),
    )

    init {
        initRenderer({ config.position }, onlyOnIsland = IslandType.GARDEN) { shouldShowDisplay() }
    }

    override fun startSessionUptime() {
        super.startSessionUptime()
        GardenUptimeManager.resumeAfkTracking()
    }

    @HandleEvent
    private fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.perTrackerConfig.trackerConfig.defaultDisplayMode,
            SkyHanniMod.feature.misc.tracker.defaultDisplayMode,
        ) {
            trackerDisplayConfig.defaultDisplayMode.get().mode?.let {
                SkyHanniTracker.storedTrackers[name] = it
            }
            displayMode = null
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onToolChange(event: GardenToolChangeEvent) {
        currentToolHasBountiful = event.toolItem?.getReforgeModifier() == "bountiful"
        firstUpdate()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onPurseChange(event: PurseChangeEvent) {
        if (!shouldTrack(TrackedSource.BOUNTIFUL)) return
        if (!currentToolHasBountiful) return
        if (lastFarmingActivity.passedSince() > 2.seconds) return
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL) return
        val coins = event.coins.roundToInt().takeIf { it > 0 } ?: return
        GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return

        modify {
            it.bountifulCoins += coins
        }
        addTrackedItem(TrackedSource.BOUNTIFUL, SKYBLOCK_COIN, coins.toLong(), message = false)
        markActivity()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val effect = EffectApi.getEffectFromGainedMessage(event.cleanMessage) ?: return
        val (source, internalName) = trackedPotionEffects[effect] ?: return
        if (!shouldTrack(source)) return
        addTrackedItem(source, internalName, -1L, message = false)
        markActivity()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onPestKill(event: PestKillEvent) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        modify {
            it.pestKills.addOrPut(event.pestType, 1)
        }
        if (BitsApi.bitsAvailable > 0 && PestApi.config.pestProfitTracker.includeBits.get()) {
            val bitsAmount = PestApi.KILL_BITS * BitsApi.bitsMultiplier()
            addTrackedItem(TrackedSource.PESTS, PestApi.BITS, bitsAmount.toLong(), message = false)
        }
        markActivity()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onShardGain(event: ShardGainEvent) {
        if (PestType.getByItemInternalNameOrNull(event.shardInternalName) == null) return
        addPestItem(event.shardInternalName, event.amount)
    }

    internal fun addPestItem(internalName: NeuInternalName, amount: Int, message: Boolean = true) {
        FarmingProfitTrackerCrops.rememberSpecialCropItem(internalName, amount.toLong())
        if (!shouldTrack(TrackedSource.PESTS)) return
        addTrackedItem(TrackedSource.PESTS, internalName, amount.toLong(), message = message)
        val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
        CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)?.let { crop ->
            modify {
                it.addCropAmount(crop, TrackedSource.PESTS, primitiveStack.amount * amount.toLong())
            }
        }
        markActivity()
    }

    internal fun addPestCoins(coins: Int) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        addTrackedItem(TrackedSource.PESTS, SKYBLOCK_COIN, coins.toLong(), message = false)
        markActivity()
    }

    internal fun addPestSpray(spray: SprayType, amount: Int = 1) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        modify {
            it.spraysUsed.addOrPut(spray, amount.toLong())
        }
        markActivity()
    }

    internal fun addTrackedItem(
        source: TrackedSource,
        internalName: NeuInternalName,
        amount: Long,
        message: Boolean = true,
    ) {
        if (amount == 0L) return
        var remaining = amount
        var shouldMessage = message
        while (remaining != 0L) {
            val chunk = remaining.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
            addItem(source, internalName, chunk, command = false, message = shouldMessage)
            remaining -= chunk
            shouldMessage = false
        }
    }

    internal fun markActivity() {
        lastFarmingActivity = SimpleTimeMark.now()
    }

    internal fun sessionDataHasNoFarmingData(): Boolean =
        getSharedTracker()?.get(SkyHanniTracker.DisplayMode.SESSION)?.hasNoFarmingData() == true

    internal fun shouldTrack(source: TrackedSource): Boolean =
        GardenApi.inGarden() && source in config.trackedSources

    private fun drawDisplay(data: FarmingProfitTrackerData): List<Searchable> = buildList {
        addSearchString("§e§lFarming Profit Tracker")
        addBucketSelector(this, data, "Source")

        var profit = drawItems(data, { true }, this)
        profit = addPestSprayCost(data, profit)

        FarmingProfitTrackerStats.addStats(this, data)

        val duration = data.getTotalUptime()
        val action = data.profitAction()
        addAll(addTotalProfit(profit, action.amount, action.action, duration, action.plural))

        addPriceFromButton(this)
    }

    private fun MutableList<Searchable>.addPestSprayCost(data: FarmingProfitTrackerData, profit: Double): Double {
        if (!data.isShowing(TrackedSource.PESTS)) return profit
        val spraysUsed = data.spraysUsed.filterValues { it > 0 }
        if (spraysUsed.isEmpty()) return profit

        var sprayCost = 0.0
        val hoverTips = buildList {
            spraysUsed.entries.sortedBy { it.key.displayName }.forEach { (spray, count) ->
                val price = getPricePerOrNull(spray.toInternalName())
                if (price == null) {
                    add("§7${spray.displayName}: §a${count.addSeparators()}")
                } else {
                    val total = price * count
                    sprayCost += total
                    add("§7${spray.displayName}: §a${count.addSeparators()} §7(§c-${total.shortFormat()}§7)")
                }
            }
            add("")
            add("§7Total spray cost: §c-${sprayCost.addSeparators()}")
        }

        add(
            Renderable.hoverTips(
                "§7Sprays used: §a${spraysUsed.values.sum().addSeparators()} §7(§c-${sprayCost.shortFormat()}§7)",
                hoverTips,
            ).toSearchable("Sprays used"),
        )
        return profit - sprayCost
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled || !GardenApi.inGarden()) return false
        if (!config.onlyWithFarmingTool) return true
        if (GardenApi.hasFarmingToolInHand()) return true
        return lastFarmingActivity.passedSince() < config.showAfterFarming.seconds
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetfarmingprofittracker") {
            aliases = listOf("shresetfarmingtracker")
            description = "Resets the Farming Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }
}
