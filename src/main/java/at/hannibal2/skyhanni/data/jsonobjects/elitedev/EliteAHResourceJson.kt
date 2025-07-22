package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

@KSerializable
data class EliteAuctionsResponse(
    @Expose val items: Map<NeuInternalName, List<EliteAuctionItem>>,
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

@KSerializable
data class EliteAuctionItem(
    @Expose private val skyblockId: NeuInternalName,
    @Expose val variantKey: String,
    @Expose val variedBy: EliteVariedBy,
    @Expose private val lowest: Long,
    @Expose private val lowestVolume: Int,
    @Expose private val lowest3Day: Long,
    @Expose private val lowest3DayVolume: Int,
    @Expose private val lowest7Day: Long,
    @Expose private val lowest7DayVolume: Int,
) {
    val internalName: NeuInternalName = skyblockId
    val lowestSet: EliteLowestSet = EliteLowestSet(lowest, lowestVolume)
    val lowest3DaySet: EliteLowestSet = EliteLowestSet(lowest3Day, lowest3DayVolume)
    val lowest7DaySet: EliteLowestSet = EliteLowestSet(lowest7Day, lowest7DayVolume)
}
