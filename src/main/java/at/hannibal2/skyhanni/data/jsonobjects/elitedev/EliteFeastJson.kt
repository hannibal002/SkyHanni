package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.annotations.Expose

@KSerializable
data class EliteFeastJson(
    @Expose val current: List<String>,
    @Expose val next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)
}

@KSerializable
data class EliteFeastData(
    @Expose val year: Int,
    @Expose val month: Int,
    @Expose val complete: Boolean,
    @Expose val current: List<String>,
    @Expose val next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)
}
