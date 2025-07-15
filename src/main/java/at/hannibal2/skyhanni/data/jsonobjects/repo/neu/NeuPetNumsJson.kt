package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// Pet "proper internal name" -> ...
typealias NeuPetNumsJson = Map<String, PetSpecificNums>
// Rarity -> ...
private typealias PetSpecificNums = Map<LorenzRarity, RaritySpecificNums>

@KSerializable
data class RaritySpecificNums(
    @Expose @SerializedName("1") val min: PetNums,
    @Expose @SerializedName("100") val max: PetNums,
    @Expose @SerializedName("stats_levelling_curve") private val levelCurveString: String? = null,
) {
    private val curveSplits = levelCurveString?.split(":")?.map { it.toInt() }.orEmpty()
    val minStatsLevel: Int? = curveSplits.getOrNull(0)
    val maxStatsLevel: Int? = curveSplits.getOrNull(1)
    val statLevellingType: Int? = curveSplits.getOrNull(2)

    companion object {
        class RaritySpecificNumsDeserializer : JsonDeserializer<RaritySpecificNums> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext,
            ): RaritySpecificNums {
                val obj = json.asJsonObject
                val min = context.deserialize<PetNums>(obj.getAsJsonObject("1"), PetNums::class.java)
                val max = context.deserialize<PetNums>(obj.getAsJsonObject("100"), PetNums::class.java)
                val curve = obj.get("stats_levelling_curve")?.asString
                return RaritySpecificNums(min, max, curve)
            }
        }
    }
}

data class PetNums(
    @Expose val otherNums: List<Double>,
    @Expose val statNums: Map<SkyblockStat, Double>
)
