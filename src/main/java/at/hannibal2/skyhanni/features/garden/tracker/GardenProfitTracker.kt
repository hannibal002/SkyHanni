package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.GardenProfitTrackerConfig.GardenProfitTextEntry
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.GardenProfitTracker.config
import at.hannibal2.skyhanni.features.garden.tracker.GardenProfitTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTimedBucketedItemTracker
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
        @Expose var visitorCopper: Long = 0L,
        @Expose var visitorBits: Long = 0L,
        @Expose var composterCoinsSpent: Long = 0L,
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
                            acc.merge(key, value, ::mergeBuckets)
                        }
                }
                acc
            }

        override fun GardenTrackerTypes.isBucketSelectable(): Boolean = this in GardenTrackerTypes.entries

        override fun bucketName(): String = "Type"
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> {
        val displayMap: MutableMap<GardenProfitTextEntry, Searchable> = mutableMapOf()
        val selectedBucket = bucketData.selectedBucket
        val itemList = mutableListOf<Searchable>()
        var profit = drawItems(bucketData, { true }, itemList)

        displayMap[GardenProfitTextEntry.TITLE] = Renderable.text("§e§lGarden Profit Tracker").toSearchable()
        displayMap[GardenProfitTextEntry.PROFIT_LIST] = Renderable.empty().toSearchable()

        val copper = bucketData.visitorCopper
        val copperCoins = GardenApi.config.visitors.dropsStatistics.coinsPerCopper.get() * copper

        displayMap[GardenProfitTextEntry.COPPER] =
            Renderable.text("§cCopper: ${copper.addSeparators()} ${copperCoins.formatCoin()}").toSearchable()

        val bits = bucketData.visitorBits
        val bitsCoins = GardenApi.config.visitors.dropsStatistics.coinsPerBit.get() * bits

        displayMap[GardenProfitTextEntry.BITS] =
            Renderable.text("§bBits: ${bits.addSeparators()} ${bitsCoins.formatCoin()}").toSearchable()

        displayMap[GardenProfitTextEntry.VISITOR_SPENT] =
            Renderable.text("§7Visitor Coins Spent: §6${bucketData.visitorCoinsSpent}").toSearchable()

        displayMap[GardenProfitTextEntry.COMPOSTER_SPENT] =
            Renderable.text("§7Composter Coins Spent: §6${bucketData.composterCoinsSpent}").toSearchable()

        if (selectedBucket in setOf(null, GardenTrackerTypes.VISITORS)) {
            profit += bitsCoins + copperCoins - bucketData.visitorCoinsSpent
        }

        if (selectedBucket in setOf(null, GardenTrackerTypes.COMPOSTER)) {
            profit -= bucketData.composterCoinsSpent
        }

        return formatDisplay(displayMap, bucketData, profit)
    }

    fun formatDisplay(
        displayMap: Map<GardenProfitTextEntry, Searchable>,
        data: BucketData,
        profit: Double
    ): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        val filteredMap: MutableMap<GardenProfitTextEntry, Searchable> = displayMap.toMutableMap()
        val visitorDisplaySet = setOf(GardenProfitTextEntry.COPPER, GardenProfitTextEntry.BITS, GardenProfitTextEntry.VISITOR_SPENT)
        val composterDisplaySet = setOf(GardenProfitTextEntry.COMPOSTER_SPENT)
        when (data.selectedBucket) {
            null -> {}
            GardenTrackerTypes.COMPOSTER -> visitorDisplaySet.forEach { filteredMap.remove(it) }
            GardenTrackerTypes.VISITORS -> composterDisplaySet.forEach { filteredMap.remove(it) }
            else -> (visitorDisplaySet + composterDisplaySet).forEach { filteredMap.remove(it) }
        }
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
        val pestStorage = profileStorage?.pestProfitTracker
        for (displayMode in DisplayMode.entries) {
            val entries = pestStorage?.getEntries(displayMode) ?: continue
            for (entry in entries) {
                entry
            }
        }

    }
}
