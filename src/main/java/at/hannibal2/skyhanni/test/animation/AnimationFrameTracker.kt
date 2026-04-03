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
 * frame texture. Passing null breaks the current sequence (e.g., when the entity disappears).
 *
 * LEARNING: Raw frame observations are accumulated until the same sequence of texture keys is
 * observed repeating back-to-back. The length of that repeating segment is the animation period.
 * This correctly handles animations where the same texture appears more than once per cycle,
 * since it requires the entire N-frame sequence to repeat rather than just one texture reappearing.
 * Detection requires 2 full passes; all aligned occurrences in the raw buffer seed initial samples.
 *
 * VERIFYING: On every subsequent loop the recorded frames are compared against the learned
 * sequence. Any texture mismatch increments [verificationErrors], and matching frames accumulate
 * additional tick samples for averaging. After a sequence break, the tracker seeks re-alignment
 * by finding the first accumulator whose texture key matches the resumed frame.
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

    private data class RawEntry(
        val prototype: FrameRecord,
        val serverTicks: Int,
        val clientTicks: Int,
    )

    private enum class Phase { LEARNING, VERIFYING }

    private var phase = Phase.LEARNING
    private var currentFrame: FrameRecord? = null
    private var frameStartServerTick: Long = 0L
    private var frameStartClientTick: Int = 0
    private var seeking = false

    private val rawSequence = mutableListOf<RawEntry>()
    private val accumulators = mutableListOf<FrameAccumulator>()

    /** Learned frame sequence with averaged tick durations. Empty until the first loop completes. */
    @NoReset
    val frames: List<FrameRecord> get() = accumulators.map { it.toFrameRecord() }

    /** Number of completed animation loops (≥ 1 once learning finishes). */
    var loopCount = 0
        private set

    /** Number of frames whose texture differed from the learned sequence during verification. */
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
     * Number of completed raw frame observations during LEARNING.
     * Resets to 0 once VERIFYING begins. Useful for triggering live preview before period detection.
     */
    val rawFrameCount: Int get() = rawSequence.size

    /**
     * During LEARNING, returns the completed raw frames as a live preview before period detection.
     * Each frame carries the elapsed tick count from its first observed occurrence.
     * Returns an empty list once VERIFYING begins (use [frames] instead).
     */
    val previewFrames: List<FrameRecord>
        get() {
            if (phase != Phase.LEARNING) return emptyList()
            return rawSequence.map { it.prototype.copy(serverTicks = it.serverTicks, clientTicks = it.clientTicks) }
        }

    /**
     * The learned frames in observation order. No reordering is applied; the sequence
     * reflects the animation exactly as it was first observed.
     */
    @NoReset
    val orderedFrames: List<FrameRecord> get() = frames

    /**
     * A 0-100 score combining frame completeness (50%) and timing confidence (50%).
     *
     * Frame completeness is binary: 0 until the first loop completes, 100 after.
     * Timing confidence is the minimum [FrameAccumulator.confidenceScore] across all frames.
     * For deterministic animations, reaches 100% after 3 samples per frame (1 verification loop
     * beyond the initial samples seeded by period detection).
     */
    val capturePercent: Double get() {
        val frameComplete = if (loopCount > 0) 1.0 else 0.0
        val timingConfidence = accumulators.minOfOrNull { it.confidenceScore } ?: 0.0
        return (frameComplete * 0.5 + timingConfidence * 0.5) * 100.0
    }

    /** Human-readable capture stats for debug overlays. */
    val captureStatsString: String get() =
        if (phase == Phase.LEARNING) "§7Learning... §a${rawSequence.size}§7 raw frame(s) seen"
        else "§7Captured: §a${capturePercent.roundTo(1)}%§7 " +
            "($loopCount loops, ${accumulators.size} frames, $minSampleCount samples min)"

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
        val stdDev = worst.serverStddev.roundTo(1)
        val conf = (worst.confidenceScore * 100).roundTo(1)
        return "§7Worst: #$idx (mean=${mean}t, \u00b1${stdDev}t, $n samples, conf §c$conf%§7)"
    }

    /**
     * Record the current frame texture for this tick.
     *
     * Passing null signals a sequence break (e.g., the entity disappeared between preview plays).
     * This resets the current frame so the next non-null frame starts a fresh observation run,
     * preventing a spurious transition from the last frame of one play to the first of the next.
     * In VERIFYING, a null also sets the tracker into seeking mode so it re-aligns on the
     * next completed frame rather than continuing from a stale [verifyIndex].
     *
     * @param serverTick The tick number from [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent].
     * @param clientTick The tick number from [at.hannibal2.skyhanni.api.minecraftevents.ClientEvents.totalTicks].
     * @param frame The current animation frame, or null to break the sequence.
     * @return true if a loop was just completed.
     */
    fun record(serverTick: Long, clientTick: Int, frame: FrameRecord?): Boolean {
        if (frame == null) {
            currentFrame = null
            if (phase == Phase.VERIFYING) seeking = true
            return false
        }

        if (currentFrame == null) {
            currentFrame = frame
            frameStartServerTick = serverTick
            frameStartClientTick = clientTick
            return false
        }

        val knownFrame = currentFrame ?: return false
        if (frame.textureKey == knownFrame.textureKey) return false

        val elapsedServer = (serverTick - frameStartServerTick).toInt().coerceAtLeast(1)
        val elapsedClient = (clientTick - frameStartClientTick).coerceAtLeast(1)
        val loopCompleted = onFrameEnd(knownFrame, elapsedServer, elapsedClient)
        currentFrame = frame
        frameStartServerTick = serverTick
        frameStartClientTick = clientTick
        return loopCompleted
    }

    private fun onFrameEnd(frame: FrameRecord, serverTicks: Int, clientTicks: Int): Boolean = when (phase) {
        Phase.LEARNING -> {
            rawSequence.add(RawEntry(frame, serverTicks, clientTicks))
            val period = detectPeriod()
            if (period != null) {
                buildAccumulatorsFromRaw(period)
                rawSequence.clear()
                loopCount++
                phase = Phase.VERIFYING
                verifyIndex = 0
                true
            } else false
        }

        Phase.VERIFYING -> {
            if (seeking) {
                seeking = false
                val alignedIdx = accumulators.indexOfFirst { it.prototype.textureKey == frame.textureKey }
                if (alignedIdx >= 0) {
                    accumulators[alignedIdx].serverSamples.add(serverTicks)
                    accumulators[alignedIdx].clientSamples.add(clientTicks)
                    verifyIndex = (alignedIdx + 1) % accumulators.size
                }
                // Don't count loop completion on a re-alignment tick
                false
            } else {
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

    /**
     * Scans the raw observation sequence for the smallest period L such that the last L entries
     * match the L entries immediately before them by texture key.
     *
     * Requiring the entire L-frame sequence to repeat (rather than just one texture reappearing)
     * correctly handles animations where the same texture appears more than once per cycle.
     *
     * @return The detected period, or null if no repeat has been found yet.
     */
    private fun detectPeriod(): Int? {
        val n = rawSequence.size
        for (l in 1..n / 2) {
            val offset = n - 2 * l
            var match = true
            for (i in 0 until l) {
                if (rawSequence[offset + i].prototype.textureKey != rawSequence[offset + l + i].prototype.textureKey) {
                    match = false
                    break
                }
            }
            if (match) return l
        }
        return null
    }

    /**
     * Builds accumulators from the detected period, seeding each frame with tick samples from
     * every aligned occurrence in the raw buffer, not just the last two.
     *
     * The canonical sequence is taken from the final [period] entries. Earlier entries are then
     * checked for alignment by walking backwards in [period]-sized steps; any misaligned window
     * (e.g., from a partial first cycle) is silently skipped.
     *
     * @param period The detected animation cycle length in frames.
     */
    private fun buildAccumulatorsFromRaw(period: Int) {
        val n = rawSequence.size
        val canonicalStart = n - period
        for (i in 0 until period) {
            accumulators.add(FrameAccumulator(rawSequence[canonicalStart + i].prototype))
        }
        // Walk backwards through all period-sized windows, adding samples from aligned ones.
        var occStart = canonicalStart
        while (occStart >= 0) {
            var aligned = true
            for (i in 0 until period) {
                if (rawSequence[occStart + i].prototype.textureKey != accumulators[i].prototype.textureKey) {
                    aligned = false
                    break
                }
            }
            if (aligned) {
                for (i in 0 until period) {
                    accumulators[i].serverSamples.add(rawSequence[occStart + i].serverTicks)
                    accumulators[i].clientSamples.add(rawSequence[occStart + i].clientTicks)
                }
            }
            occStart -= period
        }
    }
}
