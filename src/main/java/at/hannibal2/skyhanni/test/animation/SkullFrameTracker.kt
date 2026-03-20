package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.storage.Resettable

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

    /** If every learned frame shares the same server tick duration, returns that value; otherwise null. */
    val uniformServerTicks: Int? get() = _frames.firstOrNull()?.serverTicks?.takeIf {
        first -> _frames.all { it.serverTicks == first }
    }

    /** If every learned frame shares the same client tick duration, returns that value; otherwise null. */
    val uniformClientTicks: Int? get() = _frames.firstOrNull()?.clientTicks?.takeIf {
        first -> _frames.all { it.clientTicks == first }
    }

    private enum class Phase { LEARNING, VERIFYING }

    private var phase = Phase.LEARNING
    private var firstTexture: String? = null
    private var currentFrame: FrameRecord? = null
    private var frameStartServerTick: Long = 0L
    private var frameStartClientTick: Int = 0

    private val _frames = mutableListOf<FrameRecord>()

    /** Learned frame sequence. Empty until the first loop completes. */
    val frames: List<FrameRecord> get() = _frames

    /** Number of completed animation loops (≥ 1 once learning finishes). */
    var loopCount = 0
        private set

    /** Number of frames whose texture or tick count differed from the learned sequence. */
    var verificationErrors = 0
        private set

    /** Index into [frames] of the next expected frame during verification. */
    var verifyIndex = 0
        private set

    val isLearning get() = phase == Phase.LEARNING
    val hasData get() = _frames.isNotEmpty()

    /**
     * Record the current skull texture for this tick.
     *
     * @param serverTick The tick number from [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent].
     * @param clientTick The tick number from [at.hannibal2.skyhanni.events.ClientEvents.totalTicks].
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
            if (_frames.isNotEmpty() && frame.fullTexture == firstTexture) {
                // We've seen the first texture again, loop complete.
                // Verify that its tick duration matches what we learned initially.
                if (_frames.first().serverTicks != serverTicks) verificationErrors++
                loopCount++
                phase = Phase.VERIFYING
                // currentFrame is about to be set to the frame AFTER firstTexture,
                // so the next expected comparison is at index 1.
                verifyIndex = 1
                true
            } else {
                _frames.add(frame.copy(serverTicks = serverTicks, clientTicks = clientTicks))
                false
            }
        }

        Phase.VERIFYING -> {
            _frames.getOrNull(verifyIndex)?.let { expected ->
                if (expected.fullTexture != frame.fullTexture || expected.serverTicks != serverTicks) verificationErrors++
            }
            verifyIndex = (verifyIndex + 1) % _frames.size
            if (verifyIndex == 0) {
                loopCount++
                true
            } else false
        }
    }
}
