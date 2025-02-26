package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap

@SkyHanniModule
object DragonProfitTracker {
    private val config get() = SkyHanniMod.feature.combat.endIsland.dragonProfitTracker

    var dragonType: String? = null

    private val tracker = SkyHanniBucketedItemTracker(
        "Dragon Profit Tracker",
        { BucketData() },
        { it.dragonProfitTracker },
        { drawDisplay(it) },
    )

    class BucketData : BucketedItemTrackerData<DragonType>() {
        override fun getCoinName(bucket: DragonType?, item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(bucket: DragonType?, item: TrackedItem): List<String> = listOf("<no coins>")

        override fun DragonType.isBucketSelectable(): Boolean = true

        override fun resetItems() {
            dragonKills.clear()
            eyesPlaced = 0
        }

        override fun getDescription(bucket: DragonType?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalDragonCount()
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        fun getTotalDragonCount(): Long = selectedBucket?.let { dragonKills[it] } ?: dragonKills.values.sum()

        @Expose
        var dragonKills: MutableMap<DragonType, Long> = EnumMap(DragonType::class.java)

        @Expose
        var eyesPlaced: Long = 0
    }

    private fun drawDisplay(bucketData: DragonProfitTracker.BucketData): List<Searchable> = buildList {
        addSearchString("§b§lDragon Profit Tracker")
        tracker.addBucketSelector(this, bucketData, "Dragon Type")

        if (bucketData.getTotalDragonCount() == 0L) return@buildList

        var profit = tracker.drawItems(bucketData, { true }, this)

        var totalEyePrice = 0.0
        val eyePrice = NeuInternalName.fromItemNameOrNull("Summoning Eye")?.getPrice()
        if (eyePrice != null) {
            totalEyePrice = eyePrice * bucketData.eyesPlaced
            if (totalEyePrice > 0) {
                profit -= totalEyePrice
                val eyeFormat = "§7${bucketData.eyesPlaced}x §5Summoning Eye §7- §e${totalEyePrice.shortFormat()}"
                add(
                    Renderable.string(eyeFormat).toSearchable()
                )
            }
        }

        add(tracker.addTotalProfit(profit, bucketData.getTotalDragonCount(), "loot"))

        tracker.addPriceFromButton(this)
    }

    init {
        tracker.initRenderer({ config.position }) { isEnabled() }
    }

    fun isEnabled() =
        LorenzUtils.inSkyBlock && config.enabled && DragonFightAPI.inNestArea()
}
