package at.hannibal2.skyhanni.data.jsonobjects.other

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.fromJson
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EliteWeightJson(
    @Expose val selectedProfileId: String,
    @Expose val profiles: List<WeightProfile>
)

data class WeightProfile(
    @Expose val profileId: String,
    @Expose val profileName: String,
    @Expose val totalWeight: Double
)

data class EliteLeaderboardJson(
    @Expose val data: EliteLeaderboard
) {
    companion object {
        fun JsonObject.toEliteLeaderboardJson(): EliteLeaderboardJson {
            val jsonObject = JsonObject()
            jsonObject.add("data", this)
            return ConfigManager.gson.fromJson<EliteLeaderboardJson>(jsonObject)
        }
    }
}

data class EliteLeaderboard(
    @Expose val rank: Int,
    @Expose val upcomingRank: Int,
    @Expose val upcomingPlayers: List<UpcomingLeaderboardPlayer>
)

data class UpcomingLeaderboardPlayer(
    @Expose @SerializedName("ign") val name: String,
    @Expose @SerializedName("amount") val weight: Double
)
