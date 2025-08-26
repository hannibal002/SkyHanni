package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
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
)

sealed class EliteLeaderboardType {
    abstract val mode: EliteLeaderboardMode

    interface WithEnum<E : Enum<E>> {
        val enumValue: E?
    }

    data class Weight(val weight: FarmingWeight, override val mode: EliteLeaderboardMode) : EliteLeaderboardType(),WithEnum<FarmingWeight> {
        override val enumValue: FarmingWeight = weight
    }
    data class Crop(val crop: CropType, override val mode: EliteLeaderboardMode) : EliteLeaderboardType(), WithEnum<CropType> {
        override val enumValue: CropType = crop
    }
    data class Pest(val pest: PestType?, override val mode: EliteLeaderboardMode) : EliteLeaderboardType(), WithEnum<PestType> {
        override val enumValue: PestType? = pest
    }


    val lbName: String
        get() = when (this) {
            is Weight -> "${weight.apiName}${mode.lbSuffix}"
            is Crop   -> "${crop.eliteLbName}${mode.lbSuffix}"
            is Pest   -> {
                pest?.eliteLbName ?: // Only "all pests" (when pests is null) have a monthly leaderboard
                "pests${mode.lbSuffix}"
            }
        }

    override fun toString(): String {
        return when (this) {
            is Weight -> "${weight}${mode.displaySuffix}"
            is Crop   -> "${crop.name} Weight${mode.displaySuffix}"
            is Pest   -> {
                "${pest?.name ?: "Pest"} Kills${mode.displaySuffix}"
            }
        }
    }

    fun getCrop(): CropType? = when (this) {
        is Crop -> crop
        else -> null
    }
}

enum class EliteLeaderboardMode(
    val displayName: String,
    val lbSuffix: String = "",
    val displaySuffix: String = "") {
    ALL_TIME("All-Time", ),
    MONTHLY("Monthly", "-monthly", "Monthly"),
    ;

    override fun toString() = displayName
}

enum class FarmingWeight(val displayName: String, val apiName: String) {
    FARMING_WEIGHT("Farming Weight","farmingweight"),
    ;

    override fun toString(): String = displayName
}


data class EliteLeaderboard(
    @Expose val rank: Int,
    @Expose val amount: Double,
    @Expose val minAmount: Double,
    @Expose val initialAmount: Double,
    @Expose val upcomingRank: Int,
    @Expose val upcomingPlayers: List<EliteLeaderboardPlayer>,
    @Expose val previous: List<EliteLeaderboardPlayer>
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
