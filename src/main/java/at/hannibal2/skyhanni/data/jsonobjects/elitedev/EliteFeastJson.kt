package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration

@KSerializable
data class EliteFeastJson(
    @Expose val current: List<String>,
    @Expose @SerializedName("next") private val _next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    val next: Map<String, SimpleTimeMark?> = _next.mapValues { it.value?.let(SimpleTimeMark::fromUnixSeconds) }

    val isComplete = current.size == 3

    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)

    fun createData(): EliteFeastData {
        val now = SkyBlockTime.now()
        return EliteFeastData.of(
            year = now.year,
            month = now.month,
            complete = isComplete,
            current = current,
            next = next,
            isGrandFeast = isGrandFeast,
        )
    }

    companion object {
        fun of(
            current: List<String>,
            next: Map<String, SimpleTimeMark?>,
            isGrandFeast: Boolean,
        ) = EliteFeastJson(current, _next = next.mapValues { it.value?.toSeconds() }, isGrandFeast)
    }
}

@KSerializable
data class EliteFeastData(
    @Expose val year: Int,
    @Expose val month: Int,
    @Expose val complete: Boolean,
    @Expose val current: List<String>,
    @Expose @SerializedName("next") private val _next: Map<String, Long?>,
    @Expose val isGrandFeast: Boolean,
) {
    val next: Map<String, SimpleTimeMark?> = _next.mapValues { it.value?.let(SimpleTimeMark::fromUnixSeconds) }

    private val monthEndTime: SimpleTimeMark
        get() = SkyBlockTime(year, month + 1, 1).toTimeMark()

    fun getBody(): String = ApiUtils.serializeNullsGson.toJson(this)

    private fun getDurations(): List<Duration> {
        return next.mapNotNull { it.value?.takeIfInitialized()?.timeUntil() }
    }

    private fun getDuration(): Duration {
        return getDurations()
            .minByOrNull { it.inWholeMilliseconds } ?: Duration.ZERO
    }

    fun getActiveDuration(): Duration {
        if (next.values.all { it == null }) return monthEndTime.timeUntil()

        return getDurations()
            .filter(Duration::isPositive)
            .minByOrNull { it.inWholeMilliseconds }
            ?: monthEndTime.timeUntil()
    }

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

    companion object {
        fun of(
            year: Int,
            month: Int,
            complete: Boolean,
            current: List<String>,
            next: Map<String, SimpleTimeMark?>,
            isGrandFeast: Boolean,
        ) = EliteFeastData(
            year,
            month,
            complete,
            current,
            _next = next.mapValues { it.value?.toSeconds() },
            isGrandFeast,
        )
    }
}
