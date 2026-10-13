package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.tracker.RareCropTracker.RareCropDropType
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap

data class FarmingProfitTrackerData(
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
    data class ProfitAction(val amount: Long, val action: String, val plural: String)

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
        if (internalName == PestApi.BITS) getBitsPrice() else super.getCustomPricePer(internalName, tracker)

    private fun getBitsPrice(): Double =
        if (SkyHanniMod.feature.misc.tracker.priceSource == ItemPriceSource.NPC_SELL) 0.0
        else PestApi.config.pestProfitTracker.coinsPerBit.get().toDouble()

    override fun TrackedSource.isBucketSelectable() = true

    override fun bucketName(): String = "Source"

    fun addCropAmount(crop: CropType, source: TrackedSource, amount: Long) {
        cropAmounts.getOrPut(crop) { EnumMap<TrackedSource, Long>(TrackedSource::class.java) }.addOrPut(source, amount)
    }

    fun addBlocksBroken(crop: CropType, amount: Long) {
        blocksBroken.addOrPut(crop, amount)
    }

    fun profitAction(): ProfitAction = when (selectedBucket) {
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

internal fun FarmingProfitTrackerData.getTotalCropAmount(): Long = getCropAmountsByCrop().values.sum()

internal fun FarmingProfitTrackerData.getTotalBlocksBroken(): Long =
    if (isShowing(TrackedSource.CROPS)) blocksBroken.values.sum() else 0L

internal fun FarmingProfitTrackerData.getRareCropDropsByType(): Map<RareCropDropType, Long> {
    if (!isShowing(TrackedSource.RARE_CROPS)) return emptyMap()
    return rareCropDrops.filter { (drop, amount) -> drop != RareCropDropType.SEASONING && amount > 0 }
}

internal fun FarmingProfitTrackerData.getTotalRareCropDrops(): Long = getRareCropDropsByType().values.sum()

internal fun FarmingProfitTrackerData.getTotalSeasoningDrops(): Long =
    if (isShowing(TrackedSource.RARE_CROPS)) rareCropDrops[RareCropDropType.SEASONING] ?: 0L else 0L

internal fun FarmingProfitTrackerData.getTotalBlessedDrops(): Long =
    if (isShowing(TrackedSource.BLESSED)) blessedDrops.values.sum() else 0L

internal fun FarmingProfitTrackerData.getTotalCropFevers(): Long =
    if (isShowing(TrackedSource.CROP_FEVER)) cropFevers.values.sum() else 0L

internal fun FarmingProfitTrackerData.getTotalCropFeverDrops(): Long =
    if (isShowing(TrackedSource.CROP_FEVER)) cropFeverDrops.values.sum() else 0L

internal fun FarmingProfitTrackerData.getTotalToolExpCapsules(): Long =
    if (isShowing(TrackedSource.CROPS)) toolExpCapsules else 0L

internal fun FarmingProfitTrackerData.getTotalPestKills(): Long = if (isShowing(TrackedSource.PESTS)) {
    pestKills.entries.filter { it.key != PestType.UNKNOWN }.sumOf { it.value }
} else 0L

internal fun FarmingProfitTrackerData.getCropAmountsByCrop(): Map<CropType, Long> {
    val source = selectedBucket
    return cropAmounts.mapValues { (_, sources) ->
        source?.let { sources[it] ?: 0L } ?: sources.values.sum()
    }.filterValues { it > 0 }
}

internal fun FarmingProfitTrackerData.getCropAmountsBySource(): Map<TrackedSource, Long> {
    val result: MutableMap<TrackedSource, Long> = EnumMap(TrackedSource::class.java)
    cropAmounts.values.forEach { sources ->
        sources.forEach { (source, amount) ->
            if (isShowing(source)) result.addOrPut(source, amount)
        }
    }
    return result
}

internal fun FarmingProfitTrackerData.isShowing(source: TrackedSource) = selectedBucket == null || selectedBucket == source

internal fun FarmingProfitTrackerData.hasNoFarmingData(): Boolean =
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
