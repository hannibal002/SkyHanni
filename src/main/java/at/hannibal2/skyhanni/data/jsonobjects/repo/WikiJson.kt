package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WikiJson(
    @Expose val official: Wiki,
    @Expose val unofficial: Wiki,
)

data class Wiki(
    @Expose val name: String,
    @Expose @SerializedName(value = "url_prefix", alternate = ["urlPrefix"]) val urlPrefix: String,
    @Expose @SerializedName(value = "full_search_prefix", alternate = ["fullSearchPrefix"]) val fullSearchPrefix: String,
)
