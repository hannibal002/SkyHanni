package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MinionXPJson(
    @Expose @SerializedName("minion_xp") val minionXp: Map<String, Map<String, Double>>
)
