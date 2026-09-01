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
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestKillTimes
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
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
    trackerConfig = { SkyHanniMod.feature.garden.pests.pestProfitTracker.perTrackerConfig },
    customUptimeControl = true
) {
    val config: PestProfitTrackerConfig get() = SkyHanniMod.feature.garden.pests.pestProfitTracker

    private val patternGroup = RepoPattern.group("garden.pests.tracker")

    /**
     * REGEX-TEST: RARE DROP! Mutant Nether Wart x9 (+134)
     * REGEX-TEST: PET DROP! Slug (+78)
     * REGEX-TEST: RARE DROP! Wings of Harmony Vinyl (+139.5)
     * REGEX-TEST: RARE DROP! Not Just a Pest Vinyl (Cocoaleech)
     * REGEX-TEST: RARE DROP! DynaMITES Vinyl (+130)
     * REGEX-FAIL: RARE CROP! Cane Knot (+139.5)
     */
    // Harvest Feast drops are handled elsewhere; they're added here if determined to come from a pest.
    // This pattern intentionally does not match them.
    private val pestRareDropPattern by patternGroup.pattern(
        "raredrop",
        "(?:RARE|PET) DROP! (?<item>.+?)(?: x(?<amount>[\\d,]+))? " +
            "\\((?:\\+[\\d.,]+${SkyblockStat.OVERBLOOM.hypixelIcon}|Cocoaleech)\\)",
    )

    /**
     * REGEX-TEST: WOW! [MVP+] Eisengolem found a Dung Dye!
     */
    private val dyeDropPattern by patternGroup.pattern(
        "dye.drop",
        "^WOW! (?<player>.+) found an? (?<item>.+ Dye)!$",
    )

    val DUNG_ITEM = "DUNG".toInternalName()
    val ENCHANTED_SUNFLOWER_ITEM = "ENCHANTED_SUNFLOWER".toInternalName()
    val OVERCLOCKER = "OVERCLOCKER_3000".toInternalName()
    val DUNG_DYE = "DYE_DUNG".toInternalName()

    private val noMessageDrops = setOf(
        "PESTERMINATOR;1",
        "ULTIMATE_SUNSET;1",
    ).toInternalNames()

    data class BucketData(
        @Expose private var totalPestsKills: Long = 0L,
        @Expose var pestKills: MutableMap<PestType, Long> = EnumMap(PestType::class.java),
        @Expose var spraysUsed: MutableMap<SprayType, Long> = EnumMap(SprayType::class.java),
    ) : BucketedItemTrackerData<PestType, SessionUptime.Garden>(PestType::class, SessionUptime.Garden::class) {
        override fun getDescription(bucket: PestType?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalPestCount()
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*, *>): Double {
            return if (internalName == PestApi.BITS) {
                getBitsPrice()
            } else {
                super.getCustomPricePer(internalName, tracker)
            }
        }

        private fun getBitsPrice(): Double {
            return if (SkyHanniMod.feature.misc.tracker.priceSource == ItemPriceSource.NPC_SELL) 0.0 else config.coinsPerBit.get()
                .toDouble()
        }

        override val selectedBucketItems
            get() = if (config.includeBits.get()) super.selectedBucketItems
            else super.selectedBucketItems.filter { it.key != PestApi.BITS }
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

    private fun SprayType.addSprayUsed(amount: Int = 1) = modify { it.spraysUsed.addOrPut(this, amount.toLong()) }

    fun addRareCropDrop(drop: RareCropTracker.RareCropDropType) {
        if (!drop.canDropFromPests) return
        if (!PestApi.hasVacuumOrLassoInHand()) return

        val internalName = NeuInternalName.fromItemNameOrInternalName(drop.dropName)
        addItem(drop.pestType ?: PestType.UNKNOWN, internalName, 1, command = false)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onItemAdd(event: ItemAddEvent) {
        if (config.enabled && event.source == ItemAddManager.Source.COMMAND) {
            event.addItemFromEvent()
            return
        }

        if (event.source == ItemAddManager.Source.ITEM_ADD &&
            event.internalName in noMessageDrops &&
            PestApi.hasVacuumOrLassoInHand()
        ) {
            val pest = PestType.getByItemInternalNameOrNull(event.internalName) ?: return
            addItem(pest, event.internalName, event.amount, false)
            FarmingProfitTracker.addPestItem(event.internalName, event.amount, message = false)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        event.checkPestChats()
        event.checkSprayChats()
    }

    @HandleEvent
    private fun onPestKill(event: PestKillEvent) {
        if (BitsApi.bitsAvailable > 0) {
            val bitsAmount = PestApi.KILL_BITS * BitsApi.bitsMultiplier()
            addItem(event.pestType, PestApi.BITS, bitsAmount.toInt(), false)
        }
    }

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(config.coinsPerBit, config.includeBits) { update() }
    }

    private fun SkyHanniChatEvent.Allow.checkPestChats() {
        PestApi.pestDeathChatPattern.matchMatcher(message) {
            val pest = PestType.getByNameOrNull(group("pest")) ?: ErrorManager.skyHanniError(
                "Could not find PestType for killed pest, please report this in the Discord.",
                "pest_name" to group("pest"),
                "full_message" to message,
            )
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            val amount = group("amount").formatInt()

            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val rawName = primitiveStack.internalName.itemNameWithoutColor
            CropType.getByNameOrNull(rawName)
                ?.addCollectionCounter(CropCollectionType.PEST_BASE, primitiveStack.amount * amount.toLong())

            if (config.hideChat) blockedReason = "pest_drop"

            addItem(pest, internalName, amount, command = false)
            FarmingProfitTracker.addPestItem(internalName, amount, message = false)

            val shouldAddKill = when (pest) {
                // Field Mice drop 6 separate items, but we only want to count the kill once
                PestType.FIELD_MOUSE -> internalName == DUNG_ITEM
                // Lunar Moths drop 3 separate crops, but we only want to count the kill once
                PestType.LUNAR_MOTH -> internalName == ENCHANTED_SUNFLOWER_ITEM
                // Overclocker drops have the same format as crop drops and causes double counting kills
                else -> internalName != OVERCLOCKER
            }
            if (shouldAddKill) addKill(pest)
        }

        pestRareDropPattern.matchStyledMatcher(chatComponent) {
            val itemComponent = componentOrThrow("item")
            val itemName = itemComponent.string
            val internalName = NeuInternalName.fromItemNameOrNull(itemName)
                ?: LorenzRarity.getByComponent(itemComponent, itemName)
                    ?.let { PetUtils.petWithRarityToInternalName(itemName, it) }
                ?: return@matchStyledMatcher
            val pest = PestType.getByItemInternalNameOrNull(internalName) ?: return@matchStyledMatcher
            val amount = matcher.group("amount")?.formatInt() ?: 1

            addPestItem(pest, internalName, amount)

            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val rawName = primitiveStack.internalName.itemNameWithoutColor

            CropType.getByNameOrNull(rawName)
                ?.addCollectionCounter(CropCollectionType.PEST_RNG, primitiveStack.amount.toLong() * amount.toLong())
            // Pests always have guaranteed loot, therefore there's no need to add kill here
        }

        dyeDropPattern.matchMatcher(cleanMessage) {
            if (!group("player").endsWith(PlayerUtils.getName())) return@matchMatcher
            if (group("item") != "Dung Dye") return@matchMatcher
            addPestItem(PestType.UNKNOWN, DUNG_DYE, 1)
        }
    }

    private fun addPestItem(pest: PestType, internalName: NeuInternalName, amount: Int) {
        addItem(pest, internalName, amount, command = false)
        FarmingProfitTracker.addPestItem(internalName, amount, message = false)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onShardGain(event: ShardGainEvent) {
        if (!event.source.isAnyOf(CHARM, HUNT)) return
        val pestType = PestType.getByItemInternalNameOrNull(event.shardInternalName) ?: return
        addItem(pestType, event.shardInternalName, event.amount, command = false)
    }

    private fun SkyHanniChatEvent.Allow.checkSprayChats() {
        GardenPlotApi.plotSprayedPattern.matchMatcher(cleanMessage) {
            val spray = SprayType.getByNameOrNull(group("spray")) ?: return@matchMatcher
            val amount = groupOrNull("amount")?.formatInt() ?: 1
            spray.addSprayUsed(amount)
            FarmingProfitTracker.addPestSpray(spray, amount)
        }
    }

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
                    val sprayString = getPricePerOrNull(spray.toInternalName())?.let { price ->
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
        if (GardenApi.isCurrentlyFarming() && config.hideWhileFarming) return false
        if (config.onlyWhenHolding.isEmpty()) return true
        val allInactive = lastPestKillTimes.all {
            it.value.passedSince() > config.timeDisplayed.seconds
        }
        return config.onlyWhenHolding.any {
            when (it) {
                PestProfitTrackerConfig.HeldItem.FARMING_TOOL -> GardenApi.hasFarmingToolInHand()
                PestProfitTrackerConfig.HeldItem.VACUUM -> PestApi.hasVacuumInHand()
                PestProfitTrackerConfig.HeldItem.SPRAYONATOR -> PestApi.hasSprayonatorInHand()
                PestProfitTrackerConfig.HeldItem.LASSO -> PestApi.hasLassoInHand()
                PestProfitTrackerConfig.HeldItem.TIMEOUT -> !allInactive
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onPurseChange(event: PurseChangeEvent) {
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL || lastPestKillTimes.isEmpty()) return
        val coins = event.coins.takeIf { it in 1000.0..10000.0 } ?: return

        // Get a list of all that have been killed in the last 2 seconds, it will
        // want to be the most recent one that was killed.
        val (pest, killTime) = lastPestKillTimes.maxByOrNull { it.value } ?: return
        if (killTime.passedSince() > 2.seconds) return
        val roundedCoins = coins.roundToInt()
        addCoins(pest, roundedCoins, command = false)
        FarmingProfitTracker.addPestCoins(roundedCoins)
    }

    @HandleEvent
    private fun onIslandJoin(event: IslandJoinEvent) {
        if (event.island != IslandType.GARDEN) return
        firstUpdate()
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetpestprofittracker") {
            description = "Resets the Pest Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
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
                    PestType.getByItemInternalNameOrNull(item) ?: PestType.UNKNOWN
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
