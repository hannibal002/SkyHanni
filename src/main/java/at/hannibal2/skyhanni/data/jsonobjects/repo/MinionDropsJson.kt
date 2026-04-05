package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MinionDropsJson(
    @Expose @SerializedName("fuel_drops") val fuelDrops: List<FuelDropEntry>,
    @Expose val minions: List<MinionDropEntry>,
) {
    data class FuelDropEntry(
        @Expose val id: NeuInternalName,
        @Expose val drops: List<NeuInternalName>,
    )

    data class MinionDropEntry(
        @Expose val id: String,
        @Expose val drops: List<NeuInternalName>,
    )
}
