package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DiscontinuedMinecraftVersionsJson(
    @Expose val versions: Map<String, DiscontinuedMinecraftVersion>? = mapOf(),
)

data class DiscontinuedMinecraftVersion(
    @Expose @SerializedName("extra_info") val extraInfo: List<String>? = null,
    @Expose @SerializedName("config_info") val configInfo: List<String>? = null,
)
