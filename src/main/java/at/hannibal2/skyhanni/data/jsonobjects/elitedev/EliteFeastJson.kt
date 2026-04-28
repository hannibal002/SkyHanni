package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class EliteFeastJson(
    @Expose val current: List<String>,
    @Expose val next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    fun getBody(): String = ConfigManager.gson.toJson(this)
}

/**
 * {
 *   "year": 487,
 *   "month": 5,
 *   "complete": false,
 *   "current": [],
 *   "next": {},
 *   "isGrandFeast": false
 * }
 *
 * {
 *   "year": 1,
 *   "month": 1,
 *   "complete": true,
 *   "current": [
 *     "string"
 *   ],
 *   "next": {
 *     "additionalProperty": null
 *   },
 *   "isGrandFeast": true
 * }
 */
@KSerializable
data class EliteFeastData(
    @Expose val year: Int,
    @Expose val month: Int,
    @Expose val complete: Boolean,
    @Expose val current: List<String>,
    @Expose val next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
)
