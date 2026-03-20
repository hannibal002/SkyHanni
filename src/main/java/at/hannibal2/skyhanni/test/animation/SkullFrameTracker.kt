package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.storage.Resettable
import kotlin.math.roundToInt

/**
 * Tracks animated skull texture frames with server-tick and client-tick precision.
 *
 * Call [record] on each [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent] with the current
 * skull texture. The tracker detects animation loops and records how many ticks each frame
 * persists before transitioning, for both server and client tick counters.
 *
 * LEARNING: Records frame textures and their tick durations for one full loop.
 * A loop is detected when the first observed texture reappears after at least one other frame.
 *
 * VERIFYING: On every subsequent loop the recorded frames are compared against
 * the learned sequence. Any texture or tick-count mismatch increments [verificationErrors].
 */
class SkullFrameTracker : Resettable {

    data class FrameRecord(
        val uuid: String? = null,
        val texture: String,
        val signature: String? = null,
        val clientTicks: Int,
        val serverTicks: Int,
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
    }

    /** If every learned frame shares the same server tick duration, returns that value; otherwise null. */
    val uniformServerTicks: Int? get() = frames.firstOrNull()?.serverTicks?.takeIf { first ->
        frames.all { it.serverTicks == first }
    }

    /** If every learned frame shares the same client tick duration, returns that value; otherwise null. */
    val uniformClientTicks: Int? get() = frames.firstOrNull()?.clientTicks?.takeIf { first ->
        frames.all { it.clientTicks == first }
    }

    private enum class Phase { LEARNING, VERIFYING }

    private var phase = Phase.LEARNING
    private var firstTexture: String? = null
    private var currentFrame: FrameRecord? = null
    private var frameStartServerTick: Long = 0L
    private var frameStartClientTick: Int = 0

    private val _accumulators = mutableListOf<FrameAccumulator>()

    /** Learned frame sequence with averaged tick durations. Empty until the first loop completes. */
    val frames: List<FrameRecord> get() = _accumulators.map { it.toFrameRecord() }

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
    val hasData get() = _accumulators.isNotEmpty()

    /** Minimum number of samples collected across all frames — the weakest link in accuracy. */
    val minSampleCount: Int get() = _accumulators.minOfOrNull { it.serverSamples.size } ?: 0

    /**
     * Record the current skull texture for this tick.
     *
     * @param serverTick The tick number from [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent].
     * @param clientTick The tick number from [at.hannibal2.skyhanni.api.minecraftevents.ClientEvents.totalTicks].
     * @param frame      The current skull frame, or null if no skull is present.
     * @return true if a loop was just completed.
     */
    fun record(serverTick: Long, clientTick: Int, frame: FrameRecord?): Boolean {
        if (frame == null) return false

        if (currentFrame == null) {
            // First frame ever seen, begin tracking.
            firstTexture = frame.fullTexture
            currentFrame = frame
            frameStartServerTick = serverTick
            frameStartClientTick = clientTick
            return false
        }

        if (frame.fullTexture == currentFrame!!.fullTexture) return false

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
            if (_accumulators.isNotEmpty() && frame.fullTexture == firstTexture) {
                // We've seen the first texture again, loop complete.
                _accumulators.first().also {
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
                _accumulators.add(FrameAccumulator(frame).also {
                    it.serverSamples.add(serverTicks)
                    it.clientSamples.add(clientTicks)
                })
                false
            }
        }

        Phase.VERIFYING -> {
            val accumulator = _accumulators.getOrNull(verifyIndex)
            if (accumulator == null || accumulator.prototype.fullTexture != frame.fullTexture) {
                verificationErrors++
            } else {
                accumulator.serverSamples.add(serverTicks)
                accumulator.clientSamples.add(clientTicks)
            }
            verifyIndex = (verifyIndex + 1) % _accumulators.size
            if (verifyIndex == 0) {
                loopCount++
                true
            } else false
        }
    }
}
