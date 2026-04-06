package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

@KSerializable
data class EliteAuctionsResponse(
    @Expose val items: Map<NeuInternalName, List<EliteVariedAuctionItem>>,
)

data class EliteLowestSet(
    @Expose val lowest: Long,
    @Expose val lowestVolume: Int,
)

data class EliteVariedBy(
    @Expose val rarity: LorenzRarity,
    @Expose val pet: String? = null,
    @Expose val petLevel: EliteVariedByPetLevel? = null,
    @Expose val extra: Map<String, String>? = null,
)

data class EliteVariedByPetLevel(
    @Expose val key: String,
    @Expose val min: Int,
    @Expose val max: Int,
)

interface EliteLowestBinBase {
    val lowest: Long
    val lowestVolume: Int
    val lowest3Day: Long
    val lowest3DayVolume: Int
    val lowest7Day: Long
    val lowest7DayVolume: Int
    val last: Long
    val rawLowest: Long

    val lowestSet: EliteLowestSet get() = EliteLowestSet(lowest, lowestVolume)
    val lowest3DaySet: EliteLowestSet get() = EliteLowestSet(lowest3Day, lowest3DayVolume)
    val lowest7DaySet: EliteLowestSet get() = EliteLowestSet(lowest7Day, lowest7DayVolume)
}

@KSerializable
data class EliteVariedAuctionItem(
    @Expose private val skyblockId: NeuInternalName,
    @Expose val variantKey: String,
    @Expose val variedBy: EliteVariedBy,
    @Expose override val lowest: Long,
    @Expose override val lowestVolume: Int,
    @Expose override val lowest3Day: Long,
    @Expose override val lowest3DayVolume: Int,
    @Expose override val lowest7Day: Long,
    @Expose override val lowest7DayVolume: Int,
    @Expose override val last: Long,
    @Expose override val rawLowest: Long,
) : EliteLowestBinBase {
    val internalName: NeuInternalName = skyblockId
}

@KSerializable
data class EliteAuctionPricing(
    @Expose override val lowest: Long,
    @Expose override val lowestVolume: Int,
    @Expose override val lowest3Day: Long,
    @Expose override val lowest3DayVolume: Int,
    @Expose override val lowest7Day: Long,
    @Expose override val lowest7DayVolume: Int,
    @Expose override val last: Long,
    @Expose override val rawLowest: Long,
) : EliteLowestBinBase
