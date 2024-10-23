package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EventsJson(
    @Expose @SerializedName("great_spook") val greatSpook: Map<String, Long>,
)
