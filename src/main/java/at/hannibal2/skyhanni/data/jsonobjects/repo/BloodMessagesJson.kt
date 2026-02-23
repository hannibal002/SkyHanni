package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class BloodMessagesJson(
    @Expose @SerializedName("started") val startMessages: List<String>,
    @Expose @SerializedName("moving") val moveMessages: List<String>,
)
