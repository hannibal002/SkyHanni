package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.minutes

@KSerializable
data class EliteContestsResponse(
    @Expose val year: Int,
    @Expose val count: Int,
    @Expose val complete: Boolean,
    @Expose private val contests: Map<String, List<String>>,
) {
    @Suppress("Deprecation")
    val responseContests: List<EliteFarmingContest> = contests.mapNotNull { (timestampStr, cropStrList) ->
        val longTimeStamp = timestampStr.toLongOrNull() ?: return@mapNotNull null
        val crops = cropStrList.mapNotNull { cropStr ->
            CropType.getByNameOrNull(cropStr)
        }.takeIf { it.size == 3 } ?: return@mapNotNull null
        EliteFarmingContest((longTimeStamp * 1000).asTimeMark(), crops)
    }
}

data class EliteContestsRequest(
    val list: List<EliteFarmingContest>,
) : Map<Long, List<String>> by list.associate(
    transform = { it.startTime.toMillis() / 1000 to it.crops.map { crop -> crop.cropName } }
) {
    fun getBody(): String = ConfigManager.gson.toJson(this as Map<Long, List<String>>)
}

@KSerializable
data class EliteFarmingContest(
    @Expose val startTime: SimpleTimeMark,
    @Expose val crops: List<CropType>,
    var boostedCrop: CropType? = null,
) {
    // If hypixel changes the length of a SkyBlock day, we'll have
    // bigger problems than this being hardcoded.
    val endTime = startTime + 20.minutes
}
