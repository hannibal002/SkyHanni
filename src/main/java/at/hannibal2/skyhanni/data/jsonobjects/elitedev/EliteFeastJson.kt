package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.annotations.Expose
import kotlin.time.Duration

@KSerializable
data class EliteFeastJson(
    @Expose val current: List<String>,
    @Expose val next: Map<String, SimpleTimeMark?>,
    @Expose val isGrandFeast: Boolean,
) {
    val isComplete = current.size == 3

    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)
    fun createData(): EliteFeastData {
        val now = SkyBlockTime.now()
        return EliteFeastData(
            year = now.year,
            month = now.month,
            complete = isComplete,
            current = current,
            next = next,
            isGrandFeast = isGrandFeast,
        )
    }
}

@KSerializable
data class EliteFeastData(
    @Expose val year: Int,
    @Expose val month: Int,
    @Expose val complete: Boolean,
    @Expose val current: List<String>,
    @Expose val next: Map<String, SimpleTimeMark?>,
    @Expose val isGrandFeast: Boolean,
) {
    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)

    private fun getDurations(): List<Duration> {
        return next.map { it.value?.timeUntil() ?: Duration.INFINITE }
    }

    private fun getDuration(): Duration {
        return getDurations()
            .minByOrNull { it.inWholeMilliseconds } ?: Duration.ZERO
    }

    fun getActiveDuration(): Duration = getDurations().filter { it.isPositive() }.minByOrNull { it.inWholeMilliseconds } ?: Duration.ZERO

    fun getCurrentCrops(): List<CropType> {
        val fromCurrent = current.toCropTypes()

        if (getDuration().isNegative()) {
            val groups = next.entries.groupBy { it.value }
            val activeGroup = groups.filter { it.key?.passedSince()?.isPositive() == true }
                .minByOrNull { it.key ?: SimpleTimeMark.farFuture() }
                ?.takeIf { it.value.size == 3 }
                ?.value
                ?.map { it.key } ?: return fromCurrent
            return activeGroup.toCropTypes()
        } else return fromCurrent
    }

    private fun List<String>.toCropTypes(): List<CropType> = map { CropType.getByName(it) }
}
