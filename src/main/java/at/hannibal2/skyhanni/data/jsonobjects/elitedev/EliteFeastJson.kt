package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.annotations.Expose
import kotlin.time.Duration

@KSerializable
data class EliteFeastJson(
    @Expose val current: List<String>,
    @Expose val next: Map<String, Long?>,
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
    @Expose val next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)

    private fun getDurations(): List<Duration> {
        return next.map { it.value?.asTimeMark()?.timeUntil() ?: Duration.INFINITE }
    }

    private fun getDuration(): Duration {
        return getDurations()
            .minByOrNull { it.inWholeMilliseconds } ?: Duration.ZERO
    }

    fun getActiveDuration(): Duration = getDurations().filter { it.isPositive() }.minByOrNull { it.inWholeMilliseconds } ?: Duration.ZERO

    fun getCurrentCrops(): List<CropType> {
        val fromCurrent = current.map { CropType.getByName(it) }

        if (getDuration().isNegative()) {
            val groups = next.entries.groupBy { it.value }
            val activeGroup = groups.minByOrNull { it.key ?: Long.MAX_VALUE }
                ?.takeIf { it.value.size == 3 }
                ?.value
                ?.map { it.key } ?: return fromCurrent
            return activeGroup.map { CropType.getByName(it) }
        } else return fromCurrent
    }
}
