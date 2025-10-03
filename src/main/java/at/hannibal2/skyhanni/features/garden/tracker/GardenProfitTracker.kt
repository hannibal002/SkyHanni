package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.GardenProfitTrackerConfig.GardenProfitTextEntry
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.GardenProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPrice
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTimedBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object GardenProfitTracker : SkyHanniTimedBucketedItemTracker<GardenTrackerTypes, GardenProfitTracker.BucketData>(
    "Garden Profit Tracker",
    { BucketData() },
    { it.garden.gardenProfitTracker },
    drawDisplay = { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.garden.profitTracker.perTrackerConfig },
    customUptimeControl = true
) {
    val config get() = GardenApi.config.profitTracker
    val BITS = "skyblock_bit".toInternalName()
    val COPPER = "skyblock_copper".toInternalName()
    val HARBINGER = "POTION_HARVEST_HARBINGER;5".toInternalName()
    val STINKY_CHEESE = "POTION_STINKY_CHEESE;1".toInternalName()
    val TRUFFLE = "REFINED_DARK_CACAO_TRUFFLE".toInternalName()
    val REPELLENT = "PEST_REPELLENT".toInternalName()
    val REPELLENT_MAX = "PEST_REPELLENT_MAX".toInternalName()

    @HandleEvent
    fun onCropGain(event: CropCollectionAddEvent) {
        if (event.cropCollectionType !in setOf(CropCollectionType.BREAKING_CROPS, CropCollectionType.MOOSHROOM_COW)) return
        addItem(GardenTrackerTypes.BREAKING_CROPS, event.crop.internalName, event.amount.toInt(), false)
    }

    @HandleEvent
    fun onEffectGain(event: EffectDurationChangeEvent) {
        if (!event.justConsumed) return
        val name = when (event.effect) {
            NonGodPotEffect.HARVEST_HARBINGER -> HARBINGER
            NonGodPotEffect.DOUCE_PLUIE_DE_STINKY_CHEESE -> STINKY_CHEESE
            NonGodPotEffect.REFINED_DARK_CACAO_TRUFFLE -> TRUFFLE
            NonGodPotEffect.PEST_REPELLENT -> REPELLENT
            NonGodPotEffect.PEST_REPELLENT_MAX -> REPELLENT_MAX
            else -> null
        }
        if (name != null) addItem(GardenTrackerTypes.CONSUMABLES, name, -1, false)
    }

    class TimeData : TimedTrackerData<BucketData>({ BucketData() })

    data class BucketData(
        // these only apply to one bucket so no need to make them bucketed
        @Expose var visitorCoinsSpent: Long = 0L,
        @Expose var composterCoinsSpent: Long = 0L,
        @Expose var sprayCoinsSpent: Long = 0L,
        @Expose var blocksBroken: Long = 0L
    ) : BucketedItemTrackerData<GardenTrackerTypes, SessionUptime.Garden>(GardenTrackerTypes::class, SessionUptime.Garden::class) {
        private val excludeFromTotal: Set<NeuInternalName> =
            CropType.entries.map { it.cropName.toInternalName() }.toSet() + "Seeds".toInternalName() + SKYBLOCK_COIN

        override fun getDescription(bucket: GardenTrackerTypes?, timesGained: Long): List<String> {
            return listOf(
                "§7You have gained this §e${timesGained.addSeparators()} times."
            )
        }

        override fun getCoinName(bucket: GardenTrackerTypes?, item: TrackedItem) = when(bucket) {
            GardenTrackerTypes.PESTS -> "§6Pest Kill Coins"
            GardenTrackerTypes.BREAKING_CROPS -> "§6Bountiful Coins"
            else -> "§6Dropped Coins"
        }

        override fun getCoinDescription(bucket: GardenTrackerTypes?, item: TrackedItem): List<String> {
            val coinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7You gained §6$coinsFormat coins.",
            )
        }

        override fun flattenBucketsItems(): MutableMap<NeuInternalName, TrackedItem> =
            buckets.distinct().fold(mutableMapOf()) { acc, bucket ->
                if (bucket in config.profitTypes.get()) {
                    bucket.items.entries.distinctBy { it.key }
                        .forEach { (key, value) ->
                            // we'll add bountiful coins and crops to the crop coins amount instead of item list
                            if (shouldShowInTotal(bucket, key)) {
                                acc.merge(key, value, ::mergeBuckets)
                            }
                        }
                }
                acc
            }

        override fun GardenTrackerTypes.isBucketSelectable(): Boolean = this in GardenTrackerTypes.entries

        override fun bucketName(): String = "Type"

        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*, *>): Double {
            // bz prices for base crops are wildly inaccurate and prone to manipulation
            val npcSellItems = CropType.entries.map { it.internalName }.toSet()
            return when(internalName) {
                BITS -> config.coinsPerBit.get().toDouble()
                COPPER -> config.coinsPerCopper.get().toDouble()
                in npcSellItems -> internalName.getNpcPrice()
                else -> super.getCustomPricePer(internalName, tracker)
            }
        }

        fun getCropProfit() = excludeFromTotal.sumOf{ name ->
            (bucketedItems[GardenTrackerTypes.BREAKING_CROPS]?.get(name)?.totalAmount ?: 0) * getCustomPricePer(name, GardenProfitTracker)
        }

        fun getConsumableCost() =
            bucketedItems[GardenTrackerTypes.CONSUMABLES]?.entries?.sumOf {
                it.value.totalAmount * getCustomPricePer(it.key, GardenProfitTracker)
            }?.toLong() ?: 0L

        private fun shouldShowInTotal(bucket: GardenTrackerTypes, key: NeuInternalName): Boolean {
            if (bucket !in listOf(GardenTrackerTypes.BREAKING_CROPS, GardenTrackerTypes.CONSUMABLES)) return true
            if (bucket == GardenTrackerTypes.BREAKING_CROPS && key !in excludeFromTotal) return true
            return false
        }
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> {
        val displayMap: MutableMap<GardenProfitTextEntry, Searchable> = mutableMapOf()
        val selectedBucket = bucketData.selectedBucket
        val itemList = mutableListOf<Searchable>()
        var itemProfit = drawItems(bucketData, { true }, itemList)

        displayMap[GardenProfitTextEntry.TITLE] = Renderable.text("§6§lGarden Profit Tracker").toSearchable()

        var cropProfit = 0L

        if (bucketData.selectedBucket == null) {
            cropProfit = bucketData.getCropProfit().toLong()
            displayMap[GardenProfitTextEntry.CROP_DROPS] = Renderable.text("§eCrop NPC Profit: ${cropProfit.formatCoin()}").toSearchable()
        }
        displayMap[GardenProfitTextEntry.SPACER] = Renderable.text("").toSearchable()
        displayMap[GardenProfitTextEntry.SPACER_2] = Renderable.text("").toSearchable()

        displayMap[GardenProfitTextEntry.PROFIT_LIST] = Renderable.empty().toSearchable()
        displayMap[GardenProfitTextEntry.ITEM_PROFIT] = Renderable.text("§eItem Profit: ${itemProfit.formatCoin()}").toSearchable()

        var cropsSpent = 0L
        if (selectedBucket in setOf(null, GardenTrackerTypes.VISITORS)) {
            cropsSpent += bucketData.visitorCoinsSpent
        }
        if (selectedBucket in setOf(null, GardenTrackerTypes.COMPOSTER)) {
            cropsSpent += bucketData.composterCoinsSpent
        }
        if (selectedBucket in setOf(null, GardenTrackerTypes.CONSUMABLES)) {
            cropsSpent += bucketData.getConsumableCost()
            // crops spent will already display total cost of consumable, so avoid duplication
            displayMap[GardenProfitTextEntry.ITEM_PROFIT] = Renderable.empty().toSearchable()
            itemProfit = 0.0
        }

        if (selectedBucket in setOf(null, GardenTrackerTypes.VISITORS, GardenTrackerTypes.COMPOSTER, GardenTrackerTypes.CONSUMABLES)) {
            displayMap[GardenProfitTextEntry.COINS_SPENT] =
                Renderable.text("§eCoins Spent: ${cropsSpent.formatCoin()}").toSearchable()
        }

        val uptime = bucketData.getTotalUptime().inWholeSeconds
        if (uptime > 0L) {
            val bps = bucketData.blocksBroken.toDouble() / uptime
            displayMap[GardenProfitTextEntry.BPS] = Renderable.text("§eBlocks/Second: §b${bps.roundTo(2)}").toSearchable()
        }

        val profit = itemProfit + cropProfit + cropsSpent

        val duration = bucketData.getTotalUptime()
        val totalProfitList = addTotalProfit(profit, 0, "", duration, "")
        displayMap[GardenProfitTextEntry.TOTAL_PROFIT] = totalProfitList[0]
        displayMap[GardenProfitTextEntry.PROFIT_PER_HOUR] = totalProfitList[1]

        return formatDisplay(displayMap, bucketData)
    }

    fun formatDisplay(
        displayMap: Map<GardenProfitTextEntry, Searchable>,
        data: BucketData,
    ): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        val filteredMap: MutableMap<GardenProfitTextEntry, Searchable> = displayMap.toMutableMap()
        addBucketSelector(newList, data, "Profit Type")

        val sortedList = config.textFormat.get().mapNotNull { key -> filteredMap[key]?.let { key to it } }

        for (line in sortedList) {
            if (line.first == GardenProfitTextEntry.PROFIT_LIST) {
                val isConsumable = data.selectedBucket == GardenTrackerTypes.CONSUMABLES
                val text = if (isConsumable) "§eItems Consumed:" else "§eItems Dropped:"
                newList.addSearchString(text)
                drawItems(data, { true }, newList, positiveAmountsOnly = isConsumable)
            } else {
                newList.add(line.second)
            }
        }

        addPriceFromButton(newList)

        return newList
    }

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    fun shouldShowDisplay(): Boolean = GardenApi.inGarden() && config.enabled

    private fun importData() {
        val profileStorage = ProfileStorageData.profileSpecific?.garden
        if (profileStorage?.hasImportedProfits == true) {
            ChatUtils.userError("Already Imported Profits!")
            return
        }
        val profitTracker = profileStorage?.gardenProfitTracker
        val pestStorage = profileStorage?.pestProfitTracker
        val rareCropStorage = profileStorage?.armorDropTracker
        val composterStorage = profileStorage?.composterProfitTracker
        val bpsStorage = profileStorage?.gardenBpsTracker
        val cropCollectionStorage = profileStorage?.cropCollectionTracker
        val visitorStorage = profileStorage?.visitorDropTracker

        for (displayMode in DisplayMode.entries) {
            fun addItems(
                entries: Map<String, TrackerData<*>>,
                type: GardenTrackerTypes,
                values: (Any) -> List<Triple<NeuInternalName, Long, Long>>
            ) {
                entries.forEach { (string, data) ->
                    for ((key, amount, timesGained) in values(data)) {
                        profitTracker?.getOrPutEntry(displayMode, string)?.value?.addItem(
                            type,
                            key,
                            amount,
                            false,
                            timesGained
                        )
                    }
                }
            }

            addItems(
                pestStorage?.getEntries(displayMode) ?: continue,
                GardenTrackerTypes.PESTS
            ) { dataAny ->
                val data = dataAny as PestProfitTracker.BucketData
                data.flattenBucketsItems().map {
                    Triple(it.key, it.value.totalAmount, it.value.timesGained)
                }
            }

            addItems(
                rareCropStorage?.getEntries(displayMode) ?: continue,
                GardenTrackerTypes.BREAKING_CROPS
            ) { dataAny ->
                val data = dataAny as ArmorDropTracker.Data
                data.drops.entries.map {
                    Triple(it.key.name.toInternalName(), it.value.toLong(), it.value.toLong())
                }
            }
            val composterEntries = composterStorage?.getEntries(displayMode) ?: continue
            addItems(
                composterEntries,
                GardenTrackerTypes.COMPOSTER
            ) {dataAny ->
                val data = dataAny as ComposterProfitTracker.Data
                listOf(Triple("COMPOST".toInternalName(), data.compostGained, data.compostGained))
            }
            val visitorEntries = visitorStorage?.getEntries(displayMode) ?: continue
            addItems(
                visitorEntries,
                GardenTrackerTypes.VISITORS
            ) { dataAny ->
                val data = dataAny as VisitorDropTracker.BucketData
                val itemList = data.flattenBucketsItems().map {
                    Triple(it.key, it.value.totalAmount, it.value.timesGained)
                }.toMutableList()
                val copper = data.copper.sumAllValues().toLong()
                val bits = data.bits.sumAllValues().toLong()
                itemList.add(Triple(COPPER, copper, copper))
                itemList.add(Triple(BITS, bits, bits))
                itemList
            }

            addItems(
                cropCollectionStorage?.getEntries(displayMode) ?: continue,
                GardenTrackerTypes.BREAKING_CROPS
            ) { dataAny ->
                val data = dataAny as CropCollectionTracker.Data
                var bountifulCoins = 0L
                val itemList = data.cropCollection.map {
                    val eligibleTypes =
                        setOf(CropCollectionType.BREAKING_CROPS, CropCollectionType.MOOSHROOM_COW, CropCollectionType.DICER)
                    val cropAmount =
                        it.value.cropCollectionType.filter { type -> type.key in eligibleTypes }.sumAllValues().toLong()
                    bountifulCoins += ((it.value.cropCollectionType[CropCollectionType.BREAKING_CROPS] ?: 0L) * .2).toLong()
                    Triple(it.key.internalName, cropAmount, cropAmount)
                }
                itemList + Triple(SKYBLOCK_COIN, bountifulCoins, bountifulCoins)
            }

            composterEntries.forEach { (string, data) ->
                profitTracker?.getOrPutEntry(displayMode, string)?.value?.composterCoinsSpent =
                    data.items.entries.sumOf { it.key.getNpcPrice() * it.value.totalAmount }.toLong()
            }
            visitorEntries.forEach { (string, data) ->
                profitTracker?.getOrPutEntry(displayMode, string)?.value?.visitorCoinsSpent = -data.coinsSpent.sumAllValues().toLong()
            }
            bpsStorage?.getEntries(displayMode)?.forEach { (string, data) ->
                profitTracker?.getOrPutEntry(displayMode, string)?.value?.setSessionMap(data.getSessionMap())
                profitTracker?.getOrPutEntry(displayMode, string)?.value?.blocksBroken = data.blocksBroken
            }
        }
        profileStorage?.hasImportedProfits = true
        update()
    }

    @HandleEvent
    fun onCommand(event: CommandRegistrationEvent) {
        event.register("shimportgardenprofits") {
            description = "Imports profits from other trackers to the garden profit tracker."
            category = CommandCategory.DEVELOPER_TEST
            callback { importData() }
        }
    }
}
