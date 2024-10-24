package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LevelingJson(
    @Expose @SerializedName("leveling_xp") val levelingXp: List<Int>,
    @Expose @SerializedName("leveling_caps") val levelingCaps: Map<String, Int>,
)
