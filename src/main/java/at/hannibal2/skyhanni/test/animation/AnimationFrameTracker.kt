package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.storage.NoReset
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Tracks animated texture frames with server-tick and client-tick precision.
 *
 * Call [record] on each [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent] with the current
 * frame texture. The tracker detects animation loops and records how many ticks each frame
 * persists before transitioning, for both server and client tick counters.
 *
 * LEARNING: Records frame textures and their tick durations for one full loop.
 * A loop is detected when the first observed texture reappears after at least one other frame.
 *
 * VERIFYING: On every subsequent loop the recorded frames are compared against
 * the learned sequence. Any texture or tick-count mismatch increments [verificationErrors].
 */
class AnimationFrameTracker : Resettable {

    data class FrameRecord(
        val uuid: String? = null,
        val texture: String,
        val signature: String? = null,
        val clientTicks: Int,
        val serverTicks: Int,
        /**
         * Stable key used for animation sequence comparisons.
         * Should be the decoded texture URL, which is invariant across different player profiles
         * that may serve the same visual skin. Falls back to [texture] if URL extraction fails.
         */
        val textureKey: String = texture,
    ) {
        val fullTexture get() = if (uuid != null) "$uuid:$texture" else texture
    }

    private data class FrameAccumulator(
        val prototype: FrameRecord,
        val clientSamples: MutableList<Int> = mutableListOf(),
        val serverSamples: MutableList<Int> = mutableListOf(),
    ) {
        fun toFrameRecord() = prototype.copy(
            clientTicks = clientSamples.average().roundToInt(),
            serverTicks = serverSamples.average().roundToInt(),
        )

        val serverMean: Double get() = serverSamples.average()

        val serverStddev: Double get() {
            val n = serverSamples.size
            if (n < 2) return 0.0
            val mean = serverMean
            return sqrt(serverSamples.sumOf { s -> (s - mean) * (s - mean) } / n)
        }

        /**
         * A 0-1 confidence score combining sample count (capping at 3) and tick consistency.
         *
         * For deterministic animations (all samples identical), three observations yield 1.0.
         * Higher variance between samples reduces the score, requiring more loops to compensate.
         * The coefficient of variation (stddev / mean) measures relative spread.
         */
        val confidenceScore: Double get() {
            val n = serverSamples.size
            if (n == 0) return 0.0
            val sampleFactor = minOf(1.0, n / 3.0)
            if (n < 2) return sampleFactor
            val mean = serverMean
            if (mean == 0.0) return sampleFactor
            val cv = serverStddev / mean
            val consistency = 1.0 / (1.0 + 4.0 * cv)
            return sampleFactor * consistency
        }
    }

    private enum class Phase { LEARNING, VERIFYING }

    private var phase = Phase.LEARNING
    private var firstTexture: String? = null
    private var currentFrame: FrameRecord? = null
    private var frameStartServerTick: Long = 0L
    private var frameStartClientTick: Int = 0

    private val accumulators = mutableListOf<FrameAccumulator>()

    /** Learned frame sequence with averaged tick durations. Empty until the first loop completes. */
    @NoReset
    val frames: List<FrameRecord> get() = accumulators.map { it.toFrameRecord() }

    /** Number of completed animation loops (≥ 1 once learning finishes). */
    var loopCount = 0
        private set

    /** Number of frames whose texture differed from the learned sequence. */
    var verificationErrors = 0
        private set

    /** Index into accumulators of the next expected frame during verification. */
    var verifyIndex = 0
        private set

    val isLearning get() = phase == Phase.LEARNING
    val hasData get() = accumulators.isNotEmpty()

    /** Minimum number of samples collected across all frames - the weakest link in accuracy. */
    val minSampleCount: Int get() = accumulators.minOfOrNull { it.serverSamples.size } ?: 0

    /**
     * The learned frames reordered so the frame with the highest [FrameRecord.serverTicks]
     * is last. This is the "base frame" that anchors the animation.
     * The internal accumulator list is not modified; reordering is only applied here.
     */
    @NoReset
    val orderedFrames: List<FrameRecord> get() {
        if (accumulators.isEmpty()) return emptyList()
        val f = frames
        val maxTicks = f.maxOf { it.serverTicks }
        val maxIndex = f.indexOfFirst { it.serverTicks == maxTicks }
        return f.drop(maxIndex + 1) + f.take(maxIndex + 1)
    }

    /**
     * A 0-100 score combining frame completeness (50%) and timing confidence (50%).
     *
     * Frame completeness is binary: 0 until the first loop completes, 100 after.
     * Timing confidence is the minimum [FrameAccumulator.confidenceScore] across all frames.
     * For deterministic animations, reaches 100% after 3 complete loops.
     */
    val capturePercent: Double get() {
        val frameComplete = if (loopCount > 0) 1.0 else 0.0
        val timingConfidence = accumulators.minOfOrNull { it.confidenceScore } ?: 0.0
        return (frameComplete * 0.5 + timingConfidence * 0.5) * 100.0
    }

    /** Human-readable capture stats for debug overlays. */
    val captureStatsString: String get() =
        "§7Captured: §a${capturePercent.roundTo(1)}%§7 " +
            "($loopCount loops, ${accumulators.size} frames, $minSampleCount samples min)"

    /** Human-readable verification status for debug overlays. */
    val verificationStatusString: String get() =
        if (verificationErrors == 0) "§aNo errors"
        else "§c$verificationErrors verification errors"

    /**
     * Human-readable detail string for the least-confident frame.
     * Shows the frame index, mean tick duration, standard deviation, sample count, and confidence score.
     * Empty string when no frames have been collected yet.
     */
    val captureDetailString: String get() {
        if (accumulators.isEmpty()) return ""
        val worst = accumulators.minByOrNull { it.confidenceScore } ?: return ""
        val idx = accumulators.indexOf(worst) + 1
        val n = worst.serverSamples.size
        if (n == 0) return "§7Worst frame: #$idx (no samples)"
        val mean = worst.serverMean.roundTo(1)
        val stddev = worst.serverStddev.roundTo(1)
        val conf = (worst.confidenceScore * 100).roundTo(1)
        return "§7Worst: #$idx (mean=${mean}t, \u00b1${stddev}t, ${n} samples, conf §c${conf}%§7)"
    }

    /**
     * Record the current frame texture for this tick.
     *
     * @param serverTick The tick number from [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent].
     * @param clientTick The tick number from [at.hannibal2.skyhanni.api.minecraftevents.ClientEvents.totalTicks].
     * @param frame The current animation frame, or null if no texture is present.
     * @return true if a loop was just completed.
     */
    fun record(serverTick: Long, clientTick: Int, frame: FrameRecord?): Boolean {
        if (frame == null) return false

        if (currentFrame == null) {
            // First frame ever seen, begin tracking.
            firstTexture = frame.textureKey
            currentFrame = frame
            frameStartServerTick = serverTick
            frameStartClientTick = clientTick
            return false
        }

        if (frame.textureKey == currentFrame!!.textureKey) return false

        // Texture changed, finalize the frame that just ended.
        val elapsedServer = (serverTick - frameStartServerTick).toInt().coerceAtLeast(1)
        val elapsedClient = (clientTick - frameStartClientTick).coerceAtLeast(1)
        val loopCompleted = onFrameEnd(currentFrame!!, elapsedServer, elapsedClient)
        currentFrame = frame
        frameStartServerTick = serverTick
        frameStartClientTick = clientTick
        return loopCompleted
    }

    private fun onFrameEnd(frame: FrameRecord, serverTicks: Int, clientTicks: Int): Boolean = when (phase) {
        Phase.LEARNING -> {
            if (accumulators.isNotEmpty() && frame.textureKey == firstTexture) {
                // We've seen the first texture again, loop complete.
                accumulators.first().also {
                    it.serverSamples.add(serverTicks)
                    it.clientSamples.add(clientTicks)
                }
                loopCount++
                phase = Phase.VERIFYING
                // currentFrame is about to be set to the frame AFTER firstTexture,
                // so the next expected comparison is at index 1.
                verifyIndex = 1
                true
            } else {
                FrameAccumulator(frame).also {
                    accumulators.add(it)
                    it.serverSamples.add(serverTicks)
                    it.clientSamples.add(clientTicks)
                }
                false
            }
        }

        Phase.VERIFYING -> {
            val accumulator = accumulators.getOrNull(verifyIndex)
            if (accumulator == null || accumulator.prototype.textureKey != frame.textureKey) {
                verificationErrors++
            } else {
                accumulator.serverSamples.add(serverTicks)
                accumulator.clientSamples.add(clientTicks)
            }
            verifyIndex = (verifyIndex + 1) % accumulators.size
            if (verifyIndex == 0) {
                loopCount++
                true
            } else false
        }
    }
}
