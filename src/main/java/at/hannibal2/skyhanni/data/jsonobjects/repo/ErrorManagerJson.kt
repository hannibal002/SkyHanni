package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ErrorManagerJson(
    @Expose @SerializedName("break_after") val breakAfter: List<String>,
    @Expose val replacements: Map<String, String>,
    @Expose @SerializedName("entire_replacements") val entireReplacements: Map<String, String>,
    @Expose val ignored: List<String>,
)
