package at.hannibal2.skyhanni.data.jsonobjects.elitedev

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
    @Expose val cropWeight: Map<String, Double>,
    @Expose val bonusWeight: Map<String, Int>,
    @Expose val uncountedCrops: Map<String, Int>,
    @Expose val pests: Map<String, Int>,
)

enum class EliteLeaderboardType(private val displayName: String, val suffix: String = "") {
    NORMAL("Normal"),
    MONTHLY("Monthly", "-monthly"),
    ;

    override fun toString() = displayName
}

data class EliteLeaderboard(
    @Expose val rank: Int,
    @Expose val amount: Double,
    @Expose val minAmount: Double,
    @Expose val initialAmount: Double,
    @Expose val upcomingRank: Int,
    @Expose val upcomingPlayers: List<UpcomingLeaderboardPlayer>,
)

data class UpcomingLeaderboardPlayer(
    @Expose @SerializedName("ign") val name: String,
    @Expose val profile: String,
    @Expose val uuid: UUID,
    @Expose @SerializedName("amount") val weight: Double,
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
