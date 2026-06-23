package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CorpseJson(
    @Expose @SerializedName("locations") val locations: Map<MineshaftDetection.MineshaftType, List<LorenzVec>>,
)
