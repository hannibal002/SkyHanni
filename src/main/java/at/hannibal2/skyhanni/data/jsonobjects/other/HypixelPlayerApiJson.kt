package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class HypixelPlayerApiJson(
    @Expose val profiles: List<HypixelApiProfile>
)

data class HypixelApiProfile(
    @Expose val members: Map<String, HypixelApiPlayer>,
    @Expose @SerializedName("cute_name") val profileName: String
)

data class HypixelApiPlayer(
    @Expose @SerializedName("trophy_fish") val trophyFish: HypixelApiTrophyFish
)

data class HypixelApiTrophyFish(
    val totalCaught: Int,
    val caught: Map<String, Int>
)
