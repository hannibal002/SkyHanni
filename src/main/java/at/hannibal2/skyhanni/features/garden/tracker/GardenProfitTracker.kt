package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.GardenProfitTrackerConfig.GardenProfitTextEntry
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.GardenProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
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
    val BITS = "bit".toInternalName()
    val COPPER = "copper".toInternalName()

    @HandleEvent
    fun onCropGain(event: CropCollectionAddEvent) {
        if (event.cropCollectionType !in setOf(CropCollectionType.BREAKING_CROPS, CropCollectionType.MOOSHROOM_COW)) return
        val crop = event.crop.cropName.toInternalName()
        addItem(GardenTrackerTypes.BREAKING_CROPS, crop, event.amount.toInt(), false)
    }

    class TimeData : TimedTrackerData<BucketData>({ BucketData() })

    data class BucketData(
        // these only apply to one bucket so no need to make them bucketed
        @Expose var visitorCoinsSpent: Long = 0L,
        @Expose var composterCoinsSpent: Long = 0L,
        @Expose var sprayCoinsSpent: Long = 0L,
        @Expose var cropCoins: MutableMap<CropType, Long> = mutableMapOf(),
        @Expose var blocksBroken: Long = 0L
    ) : BucketedItemTrackerData<GardenTrackerTypes, SessionUptime.Garden>(GardenTrackerTypes::class, SessionUptime.Garden::class) {
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
                            // we'll add bountiful coins to the crop coins amount
                            if (key != SKYBLOCK_COIN || bucket != GardenTrackerTypes.BREAKING_CROPS) {
                                acc.merge(key, value, ::mergeBuckets)
                            }
                        }
                }
                acc
            }

        override fun GardenTrackerTypes.isBucketSelectable(): Boolean = this in GardenTrackerTypes.entries

        override fun bucketName(): String = "Type"

        override fun getCustomPricePer(internalName: NeuInternalName, tracker: SkyHanniTracker<*, *>): Double = when(internalName) {
            BITS -> config.coinsPerBit.get().toDouble()
            COPPER -> config.coinsPerCopper.get().toDouble()
            else -> super.getCustomPricePer(internalName, tracker)
        }
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> {
        val displayMap: MutableMap<GardenProfitTextEntry, Searchable> = mutableMapOf()
        val selectedBucket = bucketData.selectedBucket
        val itemList = mutableListOf<Searchable>()
        var profit = drawItems(bucketData, { true }, itemList)

        displayMap[GardenProfitTextEntry.TITLE] = Renderable.text("§e§lGarden Profit Tracker").toSearchable()
        var cropProfit = bucketData.cropCoins.sumAllValues().toLong()
        if (bucketData.selectedBucket == null) {
            cropProfit += (bucketData.getBucketedItems(GardenTrackerTypes.BREAKING_CROPS)[SKYBLOCK_COIN]?.totalAmount ?: 0L)
        }
        displayMap[GardenProfitTextEntry.CROP_DROPS] = Renderable.text("§7Crop Profit: ${cropProfit.formatCoin()}").toSearchable()

        displayMap[GardenProfitTextEntry.PROFIT_LIST] = Renderable.empty().toSearchable()

        var cropsSpent = 0L
        if (selectedBucket in setOf(null, GardenTrackerTypes.VISITORS)) {
            cropsSpent += bucketData.visitorCoinsSpent
        }
        if (selectedBucket in setOf(null, GardenTrackerTypes.COMPOSTER)) {
            cropsSpent += bucketData.composterCoinsSpent
        }

        if (selectedBucket in setOf(null, GardenTrackerTypes.VISITORS, GardenTrackerTypes.COMPOSTER)) {
            displayMap[GardenProfitTextEntry.CROPS_SPENT] = Renderable.text("§7Crops Spent: ${cropsSpent.formatCoin()}").toSearchable()
        }

        val uptime = bucketData.getTotalUptime().inWholeSeconds
        if (uptime != 0L) {
            val bps = bucketData.blocksBroken.toDouble() / uptime
            displayMap[GardenProfitTextEntry.BPS] = Renderable.text("§7Blocks/Second: §e${bps.roundTo(2)}").toSearchable()
        }

        profit += cropsSpent
        return formatDisplay(displayMap, bucketData, profit)
    }

    fun formatDisplay(
        displayMap: Map<GardenProfitTextEntry, Searchable>,
        data: BucketData,
        profit: Double
    ): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        val filteredMap: MutableMap<GardenProfitTextEntry, Searchable> = displayMap.toMutableMap()
        addBucketSelector(newList, data, "Profit Type")

        val sortedList = config.textFormat.get().mapNotNull { key -> filteredMap[key]?.let { key to it } }

        for (line in sortedList) {
            if (line.first == GardenProfitTextEntry.PROFIT_LIST) {
                drawItems(data, { true }, newList)
            } else {
                newList.add(line.second)
            }
        }

        val duration = data.getTotalUptime()

        newList.addAll(addTotalProfit(profit, 0, "", duration, ""))

        addPriceFromButton(newList)

        return newList
    }

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    fun shouldShowDisplay(): Boolean = GardenApi.inGarden() && config.enabled

    fun importData() {
        val profileStorage = ProfileStorageData.profileSpecific?.garden
        val profitTracker = profileStorage?.gardenProfitTracker
        val pestStorage = profileStorage?.pestProfitTracker
        for (displayMode in DisplayMode.entries) {
            val entries = pestStorage?.getEntries(displayMode) ?: continue
            for ((string, data) in entries) {
                val items = data.flattenBucketsItems()
                for (item in items) {
                    val profitEntry = profitTracker?.getOrPutEntry(displayMode, string)
                    profitEntry?.value?.addItem(
                        GardenTrackerTypes.PESTS,
                        item.key,
                        item.value.totalAmount,
                        false,
                        item.value.timesGained
                    )
                }
            }
        }

    }
}
