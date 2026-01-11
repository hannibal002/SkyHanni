package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestProfitTrackerConfig
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.addCollectionCounter
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.util.EnumMap
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestProfitTracker : SkyHanniBucketedItemTracker<PestType, PestProfitTracker.BucketData>(
    "Pest Profit Tracker",
    ::BucketData,
    { it.garden.pestProfitTracker },
    { drawDisplay(it) },
) {
    val config: PestProfitTrackerConfig get() = SkyHanniMod.feature.garden.pests.pestProfitTracker

    private val patternGroup = RepoPattern.group("garden.pests.tracker")

    /**
     * REGEX-TEST: §6§lRARE DROP! §9Mutant Nether Wart §6(§6+1,344☘)
     * REGEX-TEST: §6§lPET DROP! §r§5Slug §6(§6+1300☘)
     * REGEX-TEST: §6§lPET DROP! §r§6Slug §6(§6+1300☘)
     * REGEX-TEST: §6§lRARE DROP! §9Squeaky Toy §6(§6+1,549☘)
     * REGEX-TEST: §6§lRARE DROP! §6Squeaky Mousemat §6(§6+1,549☘)
     */
    private val pestRareDropPattern by patternGroup.pattern(
        "raredrop",
        "§6§l(?:RARE|PET) DROP! (?:§r)?(?<item>.+) §6\\(§6\\+.*☘\\)",
    )

    /**
     * REGEX-TEST: §a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- §r§bFR1 §r§7with §r§aPlant Matter§r§7!
     * REGEX-TEST: §a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- §r§bFR1 §r§7with §r§aDung§r§7!
     * REGEX-TEST: §a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- §r§bFR1 §r§7with §r§aHoney Jar§r§7!
     * REGEX-TEST: §a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- §r§bFR1 §r§7with §r§aTasty Cheese§r§7!
     * REGEX-TEST: §a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- §r§bFR1 §r§7with §r§aCompost§r§7!
     */
    private val sprayonatorUsedPattern by patternGroup.pattern(
        "sprayonator",
        "§a§lSPRAYONATOR! §r§7You sprayed §r§aPlot §r§7- .* §r§7with §r§.(?<spray>.*)§r§7!",
    )

    val DUNG_ITEM = "DUNG".toInternalName()
    val OVERCLOCKER = "OVERCLOCKER_3000".toInternalName()
    val BITS = "SKYBLOCK_BIT".toInternalName()
    const val KILL_BITS = 5
    private val PEST_SHARD = "ATTRIBUTE_SHARD_PEST_LUCK;1".toInternalName()
    private val lastPestKillTimes = TimeLimitedCache<PestType, SimpleTimeMark>(15.seconds)
    private var adjustmentMap: Map<PestType, Map<NeuInternalName, Int>> = mapOf()

    data class BucketData(
        @Expose private var totalPestsKills: Long = 0L,
        @Expose var pestKills: MutableMap<PestType, Long> = EnumMap(PestType::class.java),
        @Expose var spraysUsed: MutableMap<SprayType, Long> = EnumMap(SprayType::class.java),
    ) : BucketedItemTrackerData<PestType>(PestType::class) {
        override fun getDescription(bucket: PestType?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalPestCount()
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCustomPricePer(internalName: NeuInternalName): Double {
            return if (internalName == BITS) {
                getBitsPrice()
            } else {
                super.getCustomPricePer(internalName)
            }
        }

        private fun getBitsPrice(): Double {
            return if (SkyHanniMod.feature.misc.tracker.priceSource == ItemPriceSource.NPC_SELL) 0.0 else config.coinsPerBit.get()
                .toDouble()
        }

        override val selectedBucketItems
            get() = if (config.includeBits.get()) super.selectedBucketItems else super.selectedBucketItems.filter { it.key != BITS }
                .toMutableMap()

        override fun getCoinName(bucket: PestType?, item: TrackedItem) = "§6Pest Kill Coins"

        override fun getCoinDescription(bucket: PestType?, item: TrackedItem): List<String> {
            val pestsCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Killing pests gives you coins.",
                "§7You got §6$pestsCoinsFormat coins §7that way.",
            )
        }

        override fun PestType.isBucketSelectable() = this in PestType.filterableEntries

        override fun bucketName(): String {
            return "Pest"
        }

        fun getTotalPestCount(): Long = if (selectedBucket != null) pestKills[selectedBucket] ?: 0L
        else (pestKills.entries.filter { it.key != PestType.UNKNOWN }.sumOf { it.value } + totalPestsKills)
    }

    private fun SprayType.addSprayUsed() = modify { it.spraysUsed.addOrPut(this, 1) }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemAdd(event: ItemAddEvent) {
        if (config.enabled && event.source == ItemAddManager.Source.COMMAND) {
            event.addItemFromEvent()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        event.checkPestChats()
        event.checkSprayChats()
    }

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        if (BitsApi.bitsAvailable > 0) {
            val bitsAmount = KILL_BITS * BitsApi.bitsMultiplier()
            addItem(event.pestType, BITS, bitsAmount.toInt(), false)
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.coinsPerBit, config.includeBits) { update() }
    }

    private fun SkyHanniChatEvent.checkPestChats() {
        PestApi.pestDeathChatPattern.matchMatcher(message) {
            val pest = PestType.getByNameOrNull(group("pest")) ?: ErrorManager.skyHanniError(
                "Could not find PestType for killed pest, please report this in the Discord.",
                "pest_name" to group("pest"),
                "full_message" to message,
            )
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            val amount = group("amount").toInt().fixAmount(internalName, pest)

            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val rawName = primitiveStack.internalName.itemNameWithoutColor
            CropType.getByNameOrNull(rawName)
                ?.addCollectionCounter(CropCollectionType.PEST_BASE, primitiveStack.amount * amount.toLong())

            if (config.hideChat && config.enabled) blockedReason = "pest_drop"

            addItem(pest, internalName, amount, command = false)

            // Field Mice drop 6 separate items, but we only want to count the kill once
            if (pest == PestType.FIELD_MOUSE && internalName == DUNG_ITEM) addKill(pest)
            // overclocker drops have the same format as crop drops and causes double counting kills
            else if (pest != PestType.FIELD_MOUSE && internalName != OVERCLOCKER) addKill(pest)
        }
        pestRareDropPattern.matchMatcher(message) {
            val itemGroup = group("item")
            val internalName = NeuInternalName.fromItemNameOrNull(itemGroup) ?: return
            val pest = PestType.getByInternalNameItemOrNull(internalName) ?: return@matchMatcher
            val amount = 1.fixAmount(internalName, pest).also {
                if (it == 1) return@also
                // If the amount was fixed, edit the chat message to reflect the change
                val fixedString = message.replace(itemGroup, "§a${it}x $itemGroup")
                chatComponent = fixedString.asComponent()
            }

            // Happens here so that the amount is fixed independently of tracker being enabled

            addItem(pest, internalName, amount, command = false)

            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val rawName = primitiveStack.internalName.itemNameWithoutColor

            CropType.getByNameOrNull(rawName)
                ?.addCollectionCounter(CropCollectionType.PEST_RNG, primitiveStack.amount.toLong() * amount.toLong())
            // Pests always have guaranteed loot, therefore there's no need to add kill here
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onShardGain(event: ShardGainEvent) {
        if (event.shardInternalName != PEST_SHARD) return
        addItem(PestType.UNKNOWN, PEST_SHARD, event.amount, command = false)
    }

    private fun SkyHanniChatEvent.checkSprayChats() {
        sprayonatorUsedPattern.matchGroup(message, "spray")?.let {
            SprayType.getByNameOrNull(it)?.addSprayUsed()
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        adjustmentMap = event.getConstant<GardenJson>("Garden").pestRareDrops
    }

    private fun Int.fixAmount(internalName: NeuInternalName, pestType: PestType) =
        adjustmentMap.takeIf { it.isNotEmpty() }?.get(pestType)?.get(internalName) ?: this

    private fun addKill(type: PestType) {
        modify {
            it.pestKills.addOrPut(type, 1)
        }
        PestKillEvent(type).post()
        lastPestKillTimes[type] = SimpleTimeMark.now()
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {
        addSearchString("§e§lPest Profit Tracker")
        addBucketSelector(this, bucketData, "Pest Type")

        var profit = drawItems(bucketData, { true }, this)

        val selectedBucket = bucketData.selectedBucket
        val pestCount = selectedBucket?.let { bucketData.pestKills[it] } ?: bucketData.getTotalPestCount()
        val pestCountFormat = "§7${selectedBucket?.pluralName ?: "Pests"} killed: §e${pestCount.addSeparators()}"

        add(
            when {
                selectedBucket != null -> Renderable.text(pestCountFormat).toSearchable()
                else -> Renderable.hoverTips(
                    pestCountFormat,
                    buildList {
                        // Sort by A-Z in displaying real types
                        bucketData.pestKills.toList().sortedBy {
                            it.first.displayName
                        }.forEach { (type, count) ->
                            add("§7${type.pluralName}: §e${count.addSeparators()}")
                        }
                    },
                ).toSearchable()
            },
        )

        if (selectedBucket == null || selectedBucket.spray != null) {
            val applicableSprays = SprayType.getByPestTypeOrAll(selectedBucket)
            val applicableSpraysUsed = bucketData.spraysUsed.filterKeys { it in applicableSprays }
            val sumSpraysUsed = applicableSpraysUsed.values.sum()

            var sprayCosts = 0.0
            val hoverTips = if (sumSpraysUsed > 0) buildList {
                applicableSpraysUsed.forEach { (spray, count) ->
                    val sprayString = spray.toInternalName().getPriceOrNull()?.let { price ->
                        val sprayCost = price * count
                        sprayCosts += sprayCost
                        "§7${spray.displayName}: §a${count.shortFormat()} §7(§c-${sprayCost.shortFormat()}§7)"
                    } ?: add("§7${spray.displayName}: §a${count.addSeparators()}")
                    add(sprayString)
                }
                add("")
                add("§7Total spray cost: §6${sprayCosts.addSeparators()} coins")
            } else emptyList()
            profit -= sprayCosts

            val sprayCostString = if (sumSpraysUsed > 0) " §7(§c-${sprayCosts.shortFormat()}§7)" else ""
            add(
                Renderable.hoverTips(
                    "§aSprays used: §a$sumSpraysUsed$sprayCostString",
                    hoverTips,
                ).toSearchable(),
            )
        }

        val duration = bucketData.getTotalUptime()
        addAll(addTotalProfit(profit, bucketData.getTotalPestCount(), "kill", duration, "Kills"))

        addPriceFromButton(this)
    }

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled || !GardenApi.inGarden()) return false
        if (GardenApi.isCurrentlyFarming()) return false
        val allInactive = lastPestKillTimes.all {
            it.value.passedSince() > config.timeDisplayed.seconds
        }
        val notHoldingTool = !PestApi.hasVacuumInHand() && !PestApi.hasSprayonatorInHand()
        return !(allInactive && notHoldingTool)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPurseChange(event: PurseChangeEvent) {
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL || lastPestKillTimes.isEmpty()) return
        val coins = event.coins.takeIf { it in 1000.0..10000.0 } ?: return

        // Get a list of all that have been killed in the last 2 seconds, it will
        // want to be the most recent one that was killed.
        val pest = lastPestKillTimes.minByOrNull { it.value }?.key ?: return
        addCoins(pest, coins.roundToInt(), command = false)
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.GARDEN) {
            firstUpdate()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetpestprofittracker") {
            description = "Resets the Pest Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        // Move any items that are in pestProfitTracker.items as the object as a map themselves,
        // migrate them to the new format of PestType -> Drop Count. All entries will be mapped to
        // respective PestType when possible, and the rest will be moved to UNKNOWN.
        val pestTypeMap: MutableMap<NeuInternalName, PestType> = mutableMapOf()
        val pestKillCountMap: MutableMap<PestType, Long> = mutableMapOf()
        event.move(
            73,
            "#profile.garden.pestProfitTracker.items",
            "#profile.garden.pestProfitTracker.bucketedItems",
        ) { items ->
            val newItems: MutableMap<PestType, MutableMap<String, TrackedItem>> = mutableMapOf()
            val type = object : TypeToken<MutableMap<String, TrackedItem>>() {}.type
            val oldItems: MutableMap<String, TrackedItem> = ConfigManager.gson.fromJson(items, type)

            oldItems.forEach { (neuInternalName, trackedItem) ->
                val item = neuInternalName.toInternalName()
                val pest = pestTypeMap.getOrPut(item) {
                    PestType.getByInternalNameItemOrNull(item) ?: PestType.UNKNOWN
                }

                // If the map for the pest already contains this item, combine the amounts
                val storage = newItems.getOrPut(pest) { mutableMapOf() }
                val newItem = storage[neuInternalName] ?: TrackedItem()
                newItem.totalAmount += trackedItem.totalAmount
                newItem.timesGained += trackedItem.timesGained
                storage[neuInternalName] = newItem
                // If the timesGained is higher than pestKillCountMap[pest], update it
                if (pest != PestType.UNKNOWN) { // Ignore UNKNOWN, as we don't want inflated kill counts
                    pestKillCountMap[pest] = pestKillCountMap.getOrDefault(pest, 0).coerceAtLeast(newItem.timesGained)
                }
            }

            ConfigManager.gson.toJsonTree(newItems)
        }

        event.add(73, "#profile.garden.pestProfitTracker.pestKills") {
            ConfigManager.gson.toJsonTree(pestKillCountMap)
        }

        event.transform(73, "#profile.garden.pestProfitTracker.totalPestsKills") { entry ->
            // Subtract all pestKillCountMap values from the totalPestsKills
            JsonPrimitive(
                entry.asLong - pestKillCountMap.entries.filter {
                    it.key != PestType.UNKNOWN
                }.sumOf { it.value },
            )
        }
        event.move(106, "garden.pests.pestProfitTacker", "garden.pests.pestProfitTracker") { entry ->
            entry
        }
    }
}
