package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SlayerDropsJson(
    @Expose @SerializedName("main_table") val table: Map<String, Map<NeuInternalName, DropDetails>>,
    @Expose @SerializedName("extra_table") val extraTable: Map<String, Map<NeuInternalName, DropDetails>>
)

data class DropDetails(
    @Expose val xpNeeded: Int,
    @Expose val weight: Int,
    @Expose val magicFind: Boolean,
)
