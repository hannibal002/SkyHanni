package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

// Pet "proper internal name" -> ...
typealias NeuPetNumsJson = Map<String, PetSpecificNums>
// Rarity -> ...
private typealias PetSpecificNums = Map<LorenzRarity, NEURaritySpecificPetNums>

@KSerializable
data class NEURaritySpecificPetNums(
    @Expose @SerializedName("1") val min: NeuPetNums,
    @Expose @SerializedName("100") val max: NeuPetNums,
    @Expose @SerializedName("stats_levelling_curve") private val levelCurveString: String? = null,
) {
    private val curveSplits = levelCurveString?.split(":")?.map { it.toInt() }.orEmpty()
    val minStatsLevel: Int? = curveSplits.getOrNull(0)
    val maxStatsLevel: Int? = curveSplits.getOrNull(1)
    val statLevellingType: Int? = curveSplits.getOrNull(2)
}

data class NeuPetNums(
    @Expose val otherNums: List<Double>,
    @Expose val statNums: Map<SkyblockStat, Double>
)
