package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MinionDropsJson(
    @Expose @SerializedName("fuel_drops") val fuelDrops: Map<NeuInternalName, Set<NeuInternalName>>,
    @Expose val minions: Map<String, Set<NeuInternalName>>,
)
