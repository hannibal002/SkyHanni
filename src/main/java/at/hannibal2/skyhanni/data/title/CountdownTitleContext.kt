package at.hannibal2.skyhanni.data.title

import at.hannibal2.skyhanni.data.title.TitleManager.CountdownTitleDisplayType
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.now
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.inPartialSeconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class CountdownTitleContext(
    var formattedTitleText: String = "",
    var formattedSubtitleText: String? = null,
    var countdownDuration: Duration = 5.seconds,
    var displayType: CountdownTitleDisplayType = CountdownTitleDisplayType.WHOLE_SECONDS,
    var updateInterval: Duration = 1.seconds,
    /**
     * How long the title will 'stick around' for after the countdown is done.
     */
    var loomDuration: Duration = 250.milliseconds,
    var onInterval: () -> Unit = {},
    var onFinish: () -> Unit = {},
) : TitleContext() {

    override val alive get() = super.alive && (virtualEndTime?.isInFuture() == true) && isActive

    private var virtualEndTime: SimpleTimeMark? = null
    private var virtualTimeLeft: Duration = getTimeLeft()
    private val internalUpdateInterval: Duration = 100.milliseconds.takeIf { it < updateInterval } ?: updateInterval
    private var isActive: Boolean = false

    private fun String.formatCountdownString() = this
        .replace("%t", virtualTimeLeft.toString())
        .replace("%f", virtualTimeLeft.format())

    override fun getTitleText(): String = formattedTitleText.formatCountdownString()
    override fun getSubtitleText(): String? = formattedSubtitleText?.formatCountdownString()

    override fun start() {
        if (isActive) return
        isActive = true
        virtualEndTime = if (virtualEndTime == null) (now() + countdownDuration) else {
            virtualEndTime?.also {
                endTime = it + loomDuration
            }
        }
        onIntervalOutward()
        onIntervalInternal()
    }

    override fun stop() {
        isActive = false
        super.stop()
        onFinish()
    }

    override fun equals(other: Any?): Boolean = this === other || other is CountdownTitleContext && this.dataEquivalent(other)
    override fun hashCode(): Int = formattedTitleText.hashCode() * 31 + (formattedSubtitleText?.hashCode() ?: 0) * 31 +
        countdownDuration.hashCode() * 31 + displayType.hashCode() * 31 +
        updateInterval.hashCode() * 31 + loomDuration.hashCode() * 31 +
        onInterval.hashCode() * 31 + onFinish.hashCode()

    private fun dataEquivalent(other: CountdownTitleContext): Boolean = super.dataEquivalent(other) &&
        countdownDuration == other.countdownDuration &&
        displayType == other.displayType &&
        updateInterval == other.updateInterval &&
        loomDuration == other.loomDuration &&
        onInterval == other.onInterval &&
        onFinish == other.onFinish

    private fun getTimeLeft(): Duration = when (displayType) {
        CountdownTitleDisplayType.WHOLE_SECONDS -> (virtualEndTime?.timeUntil()?.inWholeSeconds ?: 0).seconds
        CountdownTitleDisplayType.PARTIAL_SECONDS -> (virtualEndTime?.timeUntil()?.inPartialSeconds ?: 0.0).seconds
    }

    // TODO instead of run delayed, use tick event or similar. inaccuracies below one tick (50 ms) are not relevant imo
    private fun onIntervalOutward() {
        if (!alive) return
        onInterval()
        DelayedRun.runDelayed(updateInterval) { onIntervalOutward() }
    }

    private fun onIntervalInternal() {
        if (!alive) return stop()
        virtualTimeLeft = if (virtualEndTime?.isInFuture() == true) getTimeLeft() else Duration.ZERO
        DelayedRun.runDelayed(internalUpdateInterval) { onIntervalInternal() }
    }

    companion object {
        fun TitleContext.fromTitleData(
            displayType: CountdownTitleDisplayType,
            updateInterval: Duration,
            loomDuration: Duration,
            discardOnWorldChange: Boolean = true,
            onInterval: () -> Unit = {},
            onFinish: () -> Unit = {},
        ) = CountdownTitleContext(
            formattedTitleText = getTitleText(),
            countdownDuration = duration,
            formattedSubtitleText = getSubtitleText(),
            displayType = displayType,
            updateInterval = updateInterval,
            loomDuration = loomDuration,
            onInterval = onInterval,
            onFinish = onFinish,
        ).apply {
            this.discardOnWorldChange = discardOnWorldChange
        }
    }
}
