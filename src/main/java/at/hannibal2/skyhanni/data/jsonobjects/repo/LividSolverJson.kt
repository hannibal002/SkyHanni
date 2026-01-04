package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LividSolverJson(
    @Expose @SerializedName("livids") val livids: Map<String, LividData>,
)
data class LividData(
    @Expose val skin: String,
    @Expose val name: String,
)
