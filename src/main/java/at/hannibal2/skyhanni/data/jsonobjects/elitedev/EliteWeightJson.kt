package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType.Crop
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType.Pest
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType.Weight
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.UUID

data class ElitePlayerWeightJson(
    @Expose val selectedProfileId: String,
    @Expose val profiles: List<WeightProfile>,
)

data class EliteWeightResponse(
    @Expose val totalWeight: Double,
    @Expose val profileId: String,
)

data class WeightProfile(
    @Expose val profileId: String,
    @Expose val profileName: String,
    @Expose val totalWeight: Double,
    @Expose val crops: Map<String, Long>,
    @Expose val cropWeight: Map<String, Double>,
    @Expose val bonusWeight: Map<String, Int>,
    @Expose val uncountedCrops: Map<String, Int>,
    @Expose val pests: Map<String, Int>,
    @Expose val lastUpdated: SimpleTimeMark,
)

data class EliteLeaderboard(
    @Expose val rank: Int,
    @Expose val amount: Double,
    @Expose val minAmount: Double,
    @Expose val initialAmount: Double,
    @Expose val upcomingRank: Int,
    @Expose val upcomingPlayers: List<EliteLeaderboardPlayer>,
    @Expose val previous: List<EliteLeaderboardPlayer>?,
    @Expose val disabled: Boolean = false,
)

data class EliteLeaderboardPlayer(
    @Expose @SerializedName("ign") val name: String,
    @Expose val profile: String,
    @Expose val uuid: UUID,
    @Expose val amount: Double,
    @Expose val mode: String? = null,
    @Expose val meta: JsonObject? = null,
)

data class EliteWeightsJson(
    @Expose val crops: Map<String, Double>,
    @Expose val pests: PestWeightData,
)

data class PestWeightData(
    @Expose val brackets: Map<Int, Int>,
    @Expose @SerializedName("values") val pestWeights: Map<String, Map<Int, Double>>,
)

sealed class EliteLeaderboardType {
    abstract val mode: EliteLeaderboardMode

    interface WithEnum<E : Enum<E>> {
        val enumValue: E?
    }

    data class Weight(
        @Expose val weight: FarmingWeight,
        @Expose override val mode: EliteLeaderboardMode
    ) : EliteLeaderboardType(),
        WithEnum<FarmingWeight> {
        override val enumValue: FarmingWeight = weight
        override fun toString() = "${weight}${mode.displaySuffix}"
    }

    data class Crop(
        @Expose val crop: CropType,
        @Expose override val mode: EliteLeaderboardMode
    ) : EliteLeaderboardType(), WithEnum<CropType> {
        override val enumValue: CropType = crop
        override fun toString() = "${crop.cropName} Collection${mode.displaySuffix}"
    }

    data class Pest(
        @Expose val pest: PestType?,
        @Expose override val mode: EliteLeaderboardMode
    ) : EliteLeaderboardType(), WithEnum<PestType> {
        override val enumValue: PestType? = pest
        override fun toString() = "${pest?.displayName ?: "Pest"} Kills${mode.displaySuffix}"
    }

    val lbName: String
        get() = when (this) {
            is Weight -> "${weight.apiName}${mode.lbSuffix}"
            is Crop -> "${crop.eliteLbName}${mode.lbSuffix}"
            is Pest -> "${pest?.eliteLbName ?: "pests"}${mode.lbSuffix}"
        }

    val type
        get() = when (this) {
            is Weight -> this.weight
            is Crop -> this.crop
            is Pest -> this.pest
        }
}

val EliteLeaderboardType.pest: PestType?
    get() = (this as? Pest)?.pest

val EliteLeaderboardType.crop: CropType?
    get() = (this as? Crop)?.crop

enum class EliteLeaderboardMode(
    val displayName: String,
    val lbSuffix: String = "",
    val displaySuffix: String = ""
) {
    ALL_TIME("All-Time"),
    MONTHLY("Monthly", "-monthly", " Monthly"),
    ;

    override fun toString() = displayName
}

enum class FarmingWeight(val displayName: String, val apiName: String) {
    FARMING_WEIGHT("Farming Weight", "farmingweight"),
    ;

    override fun toString(): String = displayName
}

class EliteLeaderboardTypeAdapter : TypeAdapter<EliteLeaderboardType>() {
    override fun write(out: JsonWriter, value: EliteLeaderboardType) {
        out.beginObject()
        when (value) {
            is Weight -> {
                out.name("type").value("weight")
                out.name("weight").value(value.weight.name)
                out.name("mode").value(value.mode.name)
            }
            is Crop -> {
                out.name("type").value("crop")
                out.name("crop").value(value.crop.name)
                out.name("mode").value(value.mode.name)
            }
            is Pest -> {
                out.name("type").value("pest")
                out.name("pest")
                if (value.pest == null) {
                    out.nullValue()
                } else {
                    out.value(value.pest.name)
                }
                out.name("mode").value(value.mode.name)
            }
        }
        out.endObject()
    }

    override fun read(reader: JsonReader): EliteLeaderboardType {
        var type: String? = null
        var mode: EliteLeaderboardMode? = null
        var weight: FarmingWeight? = null
        var crop: CropType? = null
        var pest: PestType? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "mode" -> mode = EliteLeaderboardMode.valueOf(reader.nextString())
                "weight" -> weight = FarmingWeight.valueOf(reader.nextString())
                "crop" -> crop = CropType.valueOf(reader.nextString())
                "pest" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull()
                        pest = null
                    } else {
                        pest = PestType.valueOf(reader.nextString())
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return when (type) {
            "weight" -> {
                if (weight == null || mode == null) {
                    throw JsonParseException("Missing required fields for Weight")
                }
                Weight(weight, mode)
            }
            "crop" -> {
                if (crop == null || mode == null) {
                    throw JsonParseException("Missing required fields for Crop")
                }
                Crop(crop, mode)
            }
            "pest" -> {
                if (mode == null) {
                    throw JsonParseException("Missing required fields for Pest")
                }
                Pest(pest, mode)
            }
            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}
