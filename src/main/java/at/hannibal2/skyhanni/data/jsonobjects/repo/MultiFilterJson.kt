package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MultiFilterJson(
    @Expose val equals: List<String>,
    @Expose @SerializedName(value = "starts_with", alternate = ["startsWith"]) val startsWith: List<String>,
    @Expose @SerializedName(value = "ends_with", alternate = ["endsWith"]) val endsWith: List<String>,
    @Expose val contains: List<String>,
    @Expose @SerializedName(value = "contains_word", alternate = ["containsWord"]) val containsWord: List<String>,
    @Expose val description: String,
)
