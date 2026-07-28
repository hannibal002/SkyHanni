package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.effect.EffectApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.HoeLevelDisplay
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.tracker.FarmingProfitTracker.drawDisplay
import at.hannibal2.skyhanni.features.garden.tracker.RareCropTracker.RareCropDropType
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTooltip
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.garden.visitor.VisitorPriceCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

private val pestShard = "ATTRIBUTE_SHARD_PEST_LUCK;1".toInternalName()
private val seasoningInternalName = "SEASONING".toInternalName()
private val toolExpCapsule = "TOOL_EXP_CAPSULE".toInternalName()
private val carrotColoredVinylSet = "CARROT_COLORED_VINYL_SET".toInternalName()
private val stinkyCheesePotion = "POTION_STINKY_CHEESE;1".toInternalName()
private val harvestHarbingerPotion = "POTION_HARVEST_HARBINGER;5".toInternalName()
private val sackCompactionTimeout = 30.seconds
private val farmingProfitTrackerPatternGroup = RepoPattern.group("garden.farming.profit.tracker")

private data class CropPrimitiveItem(
    val crop: CropType,
    val rawInternalName: NeuInternalName,
    val rawAmount: Long,
)

private data class PendingVisitorVinylGift(
    val slotId: Int,
    val visitorName: String,
    val created: SimpleTimeMark,
)

data class ProfitAction(val amount: Long, val action: String, val plural: String)

private fun FarmingProfitTracker.Data.hasNoFarmingData(): Boolean =
    bucketedItems.values.all { it.isEmpty() } &&
        cropAmounts.isEmpty() &&
        blocksBroken.isEmpty() &&
        rareCropDrops.isEmpty() &&
        blessedDrops.isEmpty() &&
        cropFevers.isEmpty() &&
        cropFeverDrops.isEmpty() &&
        pestKills.isEmpty() &&
        spraysUsed.isEmpty() &&
        visitorsServed == 0L &&
        visitorVinylSetsGiven == 0L &&
        visitorCopper == 0L &&
        toolExpCapsules == 0L &&
        bountifulCoins == 0L

private fun CropCollectionType.toTrackedSource(): TrackedSource? = when (this) {
    CropCollectionType.BREAKING_CROPS -> TrackedSource.CROPS
    CropCollectionType.MOOSHROOM_COW -> TrackedSource.MOOSHROOM_COW
    CropCollectionType.GREENHOUSE,
    CropCollectionType.CROP_FEVER,
    CropCollectionType.PEST_BASE,
    CropCollectionType.PEST_RNG,
    CropCollectionType.UNKNOWN,
    -> null
}

/**
 * REGEX-TEST: +20 Copper
 */
private val visitorCopperPattern by farmingProfitTrackerPatternGroup.pattern(
    "visitor.copper",
    "[+](?<amount>.*) Copper",
)

/**
 * REGEX-TEST: RARE DROP! You dropped 48x Enchanted Melon Slice!
 * REGEX-TEST: UNCOMMON DROP! You dropped 24x Enchanted Melon Slice!
 */
private val cropFeverDropPattern by farmingProfitTrackerPatternGroup.pattern(
    "cropfever.drop",
    "^(?<rarity>[\\w ]+)! You dropped (?<amount>\\d+)x (?<crop>[\\w ]+)!",
)

/**
 * REGEX-TEST: BLESSED! You found an Enchanted Nether Wart!
 * REGEX-TEST: BLESSED! You found a Cropie!
 */
private val blessedDropPattern by farmingProfitTrackerPatternGroup.pattern(
    "blessed.drop",
    "^BLESSED! You found an? (?<item>.+)!$",
)

@SkyHanniModule
object FarmingProfitTracker : SkyHanniBucketedItemTracker<TrackedSource, FarmingProfitTracker.Data>(
    "Farming Profit Tracker",
    ::Data,
    { it.garden.farmingProfitTracker },
    { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.garden.farmingProfitTracker.perTrackerConfig },
    customUptimeControl = true,
) {

    override fun formatItemAmount(amount: Long): String =
        if (amount.absoluteValue >= 1_000_000L) amount.shortFormat() else amount.addSeparators()

    private val config: FarmingProfitTrackerConfig get() = SkyHanniMod.feature.garden.farmingProfitTracker
    private val blocksBrokenCache: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private val pendingReplenishCosts: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private val cropInternalNames = mutableMapOf<CropType, NeuInternalName>()
    private val recentSpecialCropItems = mutableMapOf<NeuInternalName, Long>()
    private var lastFarmingActivity = SimpleTimeMark.farPast()
    private var lastVisitorAccept = SimpleTimeMark.farPast()
    private var pendingVisitorVinylGift: PendingVisitorVinylGift? = null
    private var currentToolHasBountiful = false
    private val trackedPotionEffects = mapOf(
        NonGodPotEffect.DOUCE_PLUIE_DE_STINKY_CHEESE to (TrackedSource.PESTS to stinkyCheesePotion),
        NonGodPotEffect.HARVEST_HARBINGER to (TrackedSource.CROPS to harvestHarbingerPotion),
    )

    data class Data(
        @Expose var cropAmounts: MutableMap<CropType, MutableMap<TrackedSource, Long>> =
            EnumMap<CropType, MutableMap<TrackedSource, Long>>(CropType::class.java),
        @Expose var blocksBroken: MutableMap<CropType, Long> = EnumMap<CropType, Long>(CropType::class.java),
        @Expose var rareCropDrops: MutableMap<RareCropDropType, Long> =
            EnumMap<RareCropDropType, Long>(RareCropDropType::class.java),
        @Expose var blessedDrops: MutableMap<NeuInternalName, Long> = mutableMapOf(),
        @Expose var cropFevers: MutableMap<CropType, Long> = EnumMap<CropType, Long>(CropType::class.java),
        @Expose var cropFeverDrops: MutableMap<RngDropEnum, Long> = EnumMap<RngDropEnum, Long>(RngDropEnum::class.java),
        @Expose var pestKills: MutableMap<PestType, Long> = EnumMap<PestType, Long>(PestType::class.java),
        @Expose var spraysUsed: MutableMap<SprayType, Long> = EnumMap<SprayType, Long>(SprayType::class.java),
        @Expose var visitorsServed: Long = 0L,
        @Expose var visitorVinylSetsGiven: Long = 0L,
        @Expose var visitorCopper: Long = 0L,
        @Expose var toolExpCapsules: Long = 0L,
        @Expose var bountifulCoins: Long = 0L,
    ) : BucketedItemTrackerData<TrackedSource, SessionUptime.Garden>(
        TrackedSource::class,
        SessionUptime.Garden::class,
    ) {
        override fun getDescription(bucket: TrackedSource?, timesGained: Long): List<String> {
            val sourceName = bucket?.displayName ?: "all farming sources"
            val totalEvents = selectedBucketItems.values.sumOf { it.timesGained }.coerceAtLeast(1)
            val share = (timesGained.toDouble() / totalEvents).coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Tracked from §e$sourceName§7.",
                "§7Recorded §e${timesGained.addSeparators()} §7times.",
                "§7Share of source entries: §c$share",
            )
        }

        override fun getCoinName(bucket: TrackedSource?, item: TrackedItem): String = when (bucket) {
            TrackedSource.BOUNTIFUL -> "§6Bountiful Coins"
            TrackedSource.PESTS -> "§6Pest Kill Coins"
            TrackedSource.VISITORS -> "§6Visitor Copper Value"
            else -> "§6Farming Coins"
        }

        override fun getCoinDescription(bucket: TrackedSource?, item: TrackedItem): List<String> {
            val coinsFormat = item.totalAmount.shortFormat()
            return when (bucket) {
                TrackedSource.BOUNTIFUL -> listOf("§7Bountiful gave you §6$coinsFormat coins§7.")
                TrackedSource.PESTS -> listOf("§7Pests gave you §6$coinsFormat coins§7.")
                TrackedSource.VISITORS -> listOf("§7Visitor copper was worth §6$coinsFormat coins§7.")
                else -> listOf("§7Farming gave you §6$coinsFormat coins§7.")
            }
        }

        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*, *>): Double =
            if (internalName == PestProfitTracker.BITS) getBitsPrice() else super.getCustomPricePer(internalName, tracker)

        private fun getBitsPrice(): Double =
            if (SkyHanniMod.feature.misc.tracker.priceSource == ItemPriceSource.NPC_SELL) 0.0
            else PestProfitTracker.config.coinsPerBit.get().toDouble()

        override fun TrackedSource.isBucketSelectable() = true

        override fun bucketName(): String = "Source"

        fun addCropAmount(crop: CropType, source: TrackedSource, amount: Long) {
            cropAmounts.getOrPut(crop) { EnumMap<TrackedSource, Long>(TrackedSource::class.java) }.addOrPut(source, amount)
        }

        fun addBlocksBroken(crop: CropType, amount: Long) {
            blocksBroken.addOrPut(crop, amount)
        }

        fun getTotalCropAmount(): Long = getCropAmountsByCrop().values.sum()

        fun getTotalBlocksBroken(): Long = if (isShowing(TrackedSource.CROPS)) {
            blocksBroken.values.sum()
        } else 0L

        fun getRareCropDropsByType(): Map<RareCropDropType, Long> {
            if (!isShowing(TrackedSource.RARE_CROPS)) return emptyMap()
            return rareCropDrops.filter { (drop, amount) -> drop != RareCropDropType.SEASONING && amount > 0 }
        }

        fun getTotalRareCropDrops(): Long = getRareCropDropsByType().values.sum()

        fun getTotalSeasoningDrops(): Long =
            if (isShowing(TrackedSource.RARE_CROPS)) rareCropDrops[RareCropDropType.SEASONING] ?: 0L else 0L

        fun getTotalBlessedDrops(): Long = if (isShowing(TrackedSource.BLESSED)) blessedDrops.values.sum() else 0L

        fun getTotalCropFevers(): Long = if (isShowing(TrackedSource.CROP_FEVER)) cropFevers.values.sum() else 0L

        fun getTotalCropFeverDrops(): Long = if (isShowing(TrackedSource.CROP_FEVER)) cropFeverDrops.values.sum() else 0L

        fun getTotalToolExpCapsules(): Long = if (isShowing(TrackedSource.CROPS)) toolExpCapsules else 0L

        fun getTotalPestKills(): Long = if (isShowing(TrackedSource.PESTS)) {
            pestKills.entries.filter { it.key != PestType.UNKNOWN }.sumOf { it.value }
        } else 0L

        fun getCropAmountsByCrop(): Map<CropType, Long> {
            val source = selectedBucket
            return cropAmounts.mapValues { (_, sources) ->
                source?.let { sources[it] ?: 0L } ?: sources.values.sum()
            }.filterValues { it > 0 }
        }

        fun getCropAmountsBySource(): Map<TrackedSource, Long> {
            val result: MutableMap<TrackedSource, Long> = EnumMap(TrackedSource::class.java)
            cropAmounts.values.forEach { sources ->
                sources.forEach { (source, amount) ->
                    if (isShowing(source)) {
                        result.addOrPut(source, amount)
                    }
                }
            }
            return result
        }

        fun isShowing(source: TrackedSource) = selectedBucket == null || selectedBucket == source

        fun profitAction(): ProfitAction =
            when (selectedBucket) {
                TrackedSource.CROPS,
                TrackedSource.MOOSHROOM_COW,
                -> ProfitAction(getTotalCropAmount(), "crop", "Crops")

                TrackedSource.PESTS -> ProfitAction(getTotalPestKills(), "kill", "Kills")
                TrackedSource.RARE_CROPS -> ProfitAction(getTotalRareCropDrops(), "drop", "Drops")
                TrackedSource.BLESSED -> ProfitAction(getTotalBlessedDrops(), "drop", "Drops")
                TrackedSource.CROP_FEVER -> cropFeverProfitAction()
                TrackedSource.BOUNTIFUL -> ProfitAction(bountifulCoins, "coin", "Coins")
                TrackedSource.VISITORS -> visitorProfitAction()
                null -> allSourceProfitAction()
            }

        private fun allSourceProfitAction(): ProfitAction = listOf(
            ProfitAction(getTotalCropAmount(), "crop", "Crops"),
            ProfitAction(getTotalToolExpCapsules(), "capsule", "Capsules"),
            visitorProfitAction(),
            ProfitAction(getTotalPestKills(), "kill", "Kills"),
            ProfitAction(getTotalCropFevers(), "fever", "Fevers"),
            ProfitAction(getTotalRareCropDrops(), "drop", "Drops"),
            ProfitAction(getTotalBlessedDrops(), "drop", "Drops"),
            ProfitAction(getTotalCropFeverDrops(), "drop", "Drops"),
            ProfitAction(bountifulCoins, "coin", "Coins"),
        ).firstOrNull { it.amount > 0 } ?: ProfitAction(0, "crop", "Crops")

        private fun cropFeverProfitAction(): ProfitAction {
            val cropFevers = getTotalCropFevers()
            return if (cropFevers > 0) ProfitAction(cropFevers, "fever", "Fevers")
            else ProfitAction(getTotalCropFeverDrops(), "drop", "Drops")
        }

        private fun visitorProfitAction(): ProfitAction {
            val visitorActions = visitorsServed + visitorVinylSetsGiven
            return ProfitAction(visitorActions, "visitor action", "Visitor Actions")
        }
    }

    init {
        initRenderer({ config.position }, onlyOnIsland = IslandType.GARDEN) { shouldShowDisplay() }
    }

    override fun startSessionUptime() {
        // GardenUptimeManager owns uptime for this tracker so data updates do not restart it after AFK pause.
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
    private fun onCropCollectionAdd(event: CropCollectionAddEvent) {
        val source = event.cropCollectionType.toTrackedSource() ?: return
        if (!shouldTrack(source)) return
        trackCropAmount(event.crop, source, event.amount)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onCropClick(event: CropClickEvent) {
        clearSessionStateIfSessionWasReset()
        blocksBrokenCache.addOrPut(event.crop, 1)
        queueReplenishCost(event)
        lastFarmingActivity = SimpleTimeMark.now()
        firstUpdate()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5) || blocksBrokenCache.isEmpty()) return
        val pending = EnumMap<CropType, Long>(blocksBrokenCache)
        blocksBrokenCache.clear()
        modify { data ->
            pending.forEach { (crop, amount) ->
                data.addBlocksBroken(crop, amount)
            }
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
        // Bountiful coins show up as mob kill gains here.
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL) return
        val coins = event.coins.roundToInt().takeIf { it > 0 } ?: return
        GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return

        modify {
            it.bountifulCoins += coins
        }
        addTrackedItem(TrackedSource.BOUNTIFUL, SKYBLOCK_COIN, coins.toLong(), message = false)
        lastFarmingActivity = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        checkPotionConsumption(event.cleanMessage)
        checkRareCropDrop(event.cleanMessage)
        checkBlessedDrop(event.cleanMessage)
        checkCropFeverStart(event.cleanMessage)
        checkCropFeverDrop(event.cleanMessage)
        checkToolExpCapsule(event.cleanMessage)
        checkVisitorCopper(event.cleanMessage.trim())
    }

    private fun checkPotionConsumption(message: String) {
        val effect = EffectApi.getEffectFromGainedMessage(message) ?: return
        val trackedPotion = trackedPotionEffects[effect] ?: return
        val (source, internalName) = trackedPotion
        if (!shouldTrack(source)) return
        addTrackedItem(source, internalName, -1L, message = false)
        lastFarmingActivity = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onPestKill(event: PestKillEvent) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        modify {
            it.pestKills.addOrPut(event.pestType, 1)
        }
        if (BitsApi.bitsAvailable > 0 && PestProfitTracker.config.includeBits.get()) {
            val bitsAmount = PestProfitTracker.KILL_BITS * BitsApi.bitsMultiplier()
            addTrackedItem(TrackedSource.PESTS, PestProfitTracker.BITS, bitsAmount.toLong(), message = false)
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onShardGain(event: ShardGainEvent) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        if (event.shardInternalName != pestShard) return
        addTrackedItem(TrackedSource.PESTS, pestShard, event.amount.toLong())
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onVisitorAccept(event: VisitorAcceptEvent) {
        if (!shouldTrack(TrackedSource.VISITORS)) return
        modify {
            it.visitorsServed++
        }
        for ((internalName, amount) in event.visitor.shoppingList) {
            addTrackedItem(TrackedSource.VISITORS, internalName, -amount.toLong(), message = false)
        }
        for (internalName in event.visitor.allRewards) {
            addTrackedItem(TrackedSource.VISITORS, internalName, 1L, message = false)
        }
        lastVisitorAccept = SimpleTimeMark.now()
        lastFarmingActivity = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onItemAdd(event: ItemAddEvent) {
        when (event.source) {
            ItemAddManager.Source.COMMAND -> if (config.enabled) event.addItemFromEvent()
            ItemAddManager.Source.ITEM_ADD,
            ItemAddManager.Source.SACKS,
            -> trackCropItemFromItemAdd(event)

            else -> Unit
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!VisitorApi.inInventory) return
        if (!shouldTrack(TrackedSource.VISITORS)) return
        val item = event.item ?: return
        if (!item.isCarrotColoredVinylSetGiveButton()) return
        val visitor = VisitorApi.getVisitor(VisitorApi.lastClickedNpc) ?: return
        val slotId = event.slot?.index ?: event.slotId
        pendingVisitorVinylGift = PendingVisitorVinylGift(slotId, visitor.visitorName, SimpleTimeMark.now())
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        val pending = pendingVisitorVinylGift ?: return
        if (!VisitorApi.inInventory || pending.created.passedSince() > 5.seconds) {
            pendingVisitorVinylGift = null
            return
        }
        val visitor = VisitorApi.getVisitor(VisitorApi.lastClickedNpc) ?: return
        if (visitor.visitorName != pending.visitorName) return
        val item = event.inventoryItems[pending.slotId] ?: return
        if (!item.isCharmedVisitorButton()) return
        if (!refreshVisitorOfferFromInventory(visitor, event)) return
        pendingVisitorVinylGift = null
        modify {
            it.visitorVinylSetsGiven++
        }
        addTrackedItem(TrackedSource.VISITORS, carrotColoredVinylSet, -1L, message = false)
        lastFarmingActivity = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN, priority = HandleEvent.LOWEST)
    private fun onSackChange(event: SackChangeEvent) {
        if (event.sackChanges.isNotEmpty()) {
            recentSpecialCropItems.clear()
        }
    }

    private fun checkVisitorCopper(message: String) {
        if (!shouldTrack(TrackedSource.VISITORS)) return
        if (lastVisitorAccept.passedSince() > 1.seconds) return
        visitorCopperPattern.matchMatcher(message) {
            val copper = group("amount").formatInt()
            val coinValue = VisitorPriceCalculator.calculateTotalReward(copper).roundToLong()
            modify {
                it.visitorCopper += copper
            }
            addTrackedItem(TrackedSource.VISITORS, SKYBLOCK_COIN, coinValue, message = false)
            lastFarmingActivity = SimpleTimeMark.now()
        }
    }

    fun addPestItem(internalName: NeuInternalName, amount: Int, message: Boolean = true) {
        rememberSpecialCropItem(internalName, amount.toLong())
        if (!shouldTrack(TrackedSource.PESTS)) return
        addTrackedItem(TrackedSource.PESTS, internalName, amount.toLong(), message = message)
        val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
        CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)?.let { crop ->
            modify {
                it.addCropAmount(crop, TrackedSource.PESTS, primitiveStack.amount * amount.toLong())
            }
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    fun addPestCoins(coins: Int) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        addTrackedItem(TrackedSource.PESTS, SKYBLOCK_COIN, coins.toLong(), message = false)
        lastFarmingActivity = SimpleTimeMark.now()
    }

    fun addRareCropItem(internalName: NeuInternalName) {
        if (!shouldTrack(TrackedSource.RARE_CROPS)) return
        addTrackedItem(TrackedSource.RARE_CROPS, internalName, 1L)
        lastFarmingActivity = SimpleTimeMark.now()
    }

    fun addPestSpray(spray: SprayType, amount: Int = 1) {
        if (!shouldTrack(TrackedSource.PESTS)) return
        modify {
            it.spraysUsed.addOrPut(spray, amount.toLong())
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    private fun checkRareCropDrop(message: String) {
        for (dropType in RareCropDropType.entries) {
            if (!dropType.chatPattern.matches(message)) continue
            if (dropType == RareCropDropType.SEASONING) {
                if (shouldTrack(TrackedSource.RARE_CROPS)) addSeasoningDrop()
                return
            }
            val internalName = NeuInternalName.fromItemNameOrNull(dropType.dropName.removeColor()) ?: dropType.name.toInternalName()
            rememberSpecialCropItem(internalName, 1)
            if (!shouldTrack(TrackedSource.RARE_CROPS)) return
            addTrackedItem(TrackedSource.RARE_CROPS, internalName, 1L)
            modify {
                it.rareCropDrops.addOrPut(dropType, 1)
            }
            lastFarmingActivity = SimpleTimeMark.now()
            return
        }
    }

    private fun addSeasoningDrop() {
        modify {
            it.rareCropDrops.addOrPut(RareCropDropType.SEASONING, 1)
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    private fun checkBlessedDrop(message: String) {
        blessedDropPattern.matchMatcher(message) {
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            rememberSpecialCropItem(internalName, 1)
            if (!shouldTrack(TrackedSource.BLESSED)) return@matchMatcher
            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val crop = CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)

            addTrackedItem(TrackedSource.BLESSED, internalName, 1L)
            modify {
                it.blessedDrops.addOrPut(internalName, 1)
                crop?.let { cropType ->
                    it.addCropAmount(cropType, TrackedSource.BLESSED, primitiveStack.amount.toLong())
                }
            }
            lastFarmingActivity = SimpleTimeMark.now()
        }
    }

    private fun checkCropFeverStart(message: String) {
        if (!shouldTrack(TrackedSource.CROP_FEVER)) return
        if (!CropFeverTracker.isCropFeverStartMessage(message)) return
        val crop = GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return
        modify {
            it.cropFevers.addOrPut(crop, 1)
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    private fun checkCropFeverDrop(message: String) {
        cropFeverDropPattern.matchMatcher(message) {
            val rarity = RngDropEnum.getByNameOrNull(group("rarity")) ?: return
            val amount = group("amount").formatInt()
            val internalName = NeuInternalName.fromItemNameOrNull(group("crop")) ?: return
            rememberSpecialCropItem(internalName, amount.toLong())
            if (!shouldTrack(TrackedSource.CROP_FEVER)) return@matchMatcher
            val crop = GardenApi.getCurrentlyFarmedCrop() ?: run {
                val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
                CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)
            } ?: return

            addTrackedItem(TrackedSource.CROP_FEVER, internalName, amount.toLong())
            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            modify {
                it.cropFeverDrops.addOrPut(rarity, 1)
                it.addCropAmount(crop, TrackedSource.CROP_FEVER, primitiveStack.amount * amount.toLong())
            }
            lastFarmingActivity = SimpleTimeMark.now()
        }
    }

    private fun checkToolExpCapsule(message: String) {
        if (!shouldTrack(TrackedSource.CROPS)) return
        if (!HoeLevelDisplay.levelUpPattern.matches(message)) return
        addTrackedItem(TrackedSource.CROPS, toolExpCapsule, 1L)
        modify {
            it.toolExpCapsules++
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    private fun trackCropAmount(crop: CropType, source: TrackedSource, amount: Long) {
        if (amount <= 0) return
        clearSessionStateIfSessionWasReset()
        val internalName = crop.getCropInternalName()
        addTrackedItem(source, internalName, amount, message = false)
        if (source == TrackedSource.CROPS) {
            applyPendingReplenishCost(crop)
        }
        modify {
            it.addCropAmount(crop, source, amount)
        }
        lastFarmingActivity = SimpleTimeMark.now()
    }

    private fun queueReplenishCost(event: CropClickEvent) {
        if (!shouldTrack(TrackedSource.CROPS)) return
        val crop = event.crop
        if (!crop.replenish) return
        if (event.itemInHand?.getHypixelEnchantments()?.containsKeys("replenish") != true) return
        pendingReplenishCosts.addOrPut(crop, crop.multiplier.toLong())
    }

    private fun applyPendingReplenishCost(crop: CropType) {
        val amount = pendingReplenishCosts.remove(crop) ?: return
        removeTrackedRawCrop(crop.getCropInternalName(), amount)
    }

    private fun trackCropItemFromItemAdd(event: ItemAddEvent) {
        if (!shouldTrack(TrackedSource.CROPS) || event.amount <= 0) return
        if (lastFarmingActivity.passedSince() > sackCompactionTimeout) return
        clearSessionStateIfSessionWasReset()

        val cropItem = event.internalName.getCropPrimitiveItemOrNull() ?: return
        val currentCrop = GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return
        if (cropItem.crop != currentCrop || cropItem.rawAmount <= 1) return
        if (cropItem.rawInternalName == event.internalName) {
            lastFarmingActivity = SimpleTimeMark.now()
            return
        }

        val consumedSpecial = consumeRecentSpecialCropItem(event.internalName, event.amount.toLong())
        val amount = event.amount.toLong() - consumedSpecial
        if (amount > 0) {
            removeTrackedRawCrop(cropItem.rawInternalName, amount * cropItem.rawAmount)
            addTrackedItem(TrackedSource.CROPS, event.internalName, amount, message = false)
            lastFarmingActivity = SimpleTimeMark.now()
        }
    }

    private fun NeuInternalName.getCropPrimitiveItemOrNull(): CropPrimitiveItem? {
        val primitiveStack = NeuItems.getPrimitiveMultiplier(this)
        val crop = CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor) ?: return null
        return CropPrimitiveItem(crop, primitiveStack.internalName, primitiveStack.amount.toLong())
    }

    private fun rememberSpecialCropItem(internalName: NeuInternalName, amount: Long) {
        if (amount <= 0) return
        internalName.getCropPrimitiveItemOrNull() ?: return
        recentSpecialCropItems.addOrPut(internalName, amount)
    }

    private fun consumeRecentSpecialCropItem(internalName: NeuInternalName, amount: Long): Long {
        val stored = recentSpecialCropItems[internalName] ?: return 0L
        val consumed = minOf(stored, amount)
        val remaining = stored - consumed
        if (remaining > 0) {
            recentSpecialCropItems[internalName] = remaining
        } else {
            recentSpecialCropItems.remove(internalName)
        }
        return consumed
    }

    private fun removeTrackedRawCrop(rawInternalName: NeuInternalName, amount: Long) {
        if (amount <= 0) return
        modify { data ->
            val rawAmount = minOf(amount, data.getTrackedCropItemAmount(rawInternalName))
            if (rawAmount <= 0) return@modify
            var remaining = -rawAmount
            while (remaining != 0L) {
                val chunk = remaining.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
                data.addItem(TrackedSource.CROPS, rawInternalName, chunk, command = true)
                remaining -= chunk
            }
        }
    }

    private fun SafeItemStack.isCarrotColoredVinylSetGiveButton(): Boolean {
        if (hoverName.string != "Gift Vinyl Set") return false
        val lore = getLoreComponent().map { it.string }
        return "Gifts a Full Carrot-Colored Vinyl Set" in lore && "Click to gift!" in lore
    }

    private fun SafeItemStack.isCharmedVisitorButton(): Boolean {
        if (!hoverName.string.contains("This Visitor has been Charmed")) return false
        return getLoreComponent().any { it.string.contains("They will bring more rewards") }
    }

    private fun refreshVisitorOfferFromInventory(visitor: VisitorApi.Visitor, event: InventoryUpdatedEvent): Boolean {
        val offerItem = event.inventoryItems[VisitorApi.ACCEPT_SLOT] ?: return false
        if (offerItem.hoverName.string != "Accept Offer") return false
        visitor.offer = VisitorApi.VisitorOffer(offerItem)
        GardenVisitorTooltip.refreshVisitorOffer(visitor)
        return true
    }

    private fun Data.getTrackedCropItemAmount(internalName: NeuInternalName): Long =
        bucketedItems[TrackedSource.CROPS]?.get(internalName)?.totalAmount?.coerceAtLeast(0L) ?: 0L

    private fun addTrackedItem(source: TrackedSource, internalName: NeuInternalName, amount: Long, message: Boolean = true) {
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

    private fun clearSessionStateIfSessionWasReset() {
        if (pendingReplenishCosts.isEmpty() && recentSpecialCropItems.isEmpty()) return
        val sessionData = getSharedTracker()?.get(SkyHanniTracker.DisplayMode.SESSION) ?: return
        if (sessionData.hasNoFarmingData()) {
            resetSessionRuntimeState()
        }
    }

    private fun resetSessionRuntimeState() {
        pendingReplenishCosts.clear()
        recentSpecialCropItems.clear()
    }

    private fun CropType.getCropInternalName(): NeuInternalName = cropInternalNames.getOrPut(this) {
        val itemName = if (this == CropType.MUSHROOM) "Red Mushroom" else cropName
        NeuInternalName.fromItemNameOrNull(itemName) ?: icon.getInternalName()
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lFarming Profit Tracker")
        addBucketSelector(this, data, "Source")

        var profit = drawItems(data, { it != seasoningInternalName }, this)
        profit = addPestSprayCost(data, profit)

        FarmingProfitTrackerStats.addStats(this, data)

        val duration = data.getTotalUptime()
        val action = data.profitAction()
        addAll(addTotalProfit(profit, action.amount, action.action, duration, action.plural))

        addPriceFromButton(this)
    }

    private fun MutableList<Searchable>.addPestSprayCost(data: Data, profit: Double): Double {
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

    private fun shouldTrack(source: TrackedSource): Boolean =
        GardenApi.inGarden() && source in config.trackedSources

    private val trackerDisplayConfig
        get() = if (config.perTrackerConfig.useUniversalConfig) {
            SkyHanniMod.feature.misc.tracker
        } else {
            config.perTrackerConfig.trackerConfig
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
