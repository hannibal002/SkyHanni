package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.storage.Resettable

/**
 * Tracks animated skull texture frames with server-tick precision.
 *
 * Call [record] on each [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent] with the current
 * skull texture. The tracker detects animation loops and records how many server ticks each frame
 * persists before transitioning.
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
        val ticks: Int,
    ) {
        val fullTexture get() = if (uuid != null) "$uuid:$texture" else texture
    }

    private enum class Phase {
        LEARNING,
        VERIFYING
    }

    private var phase = Phase.LEARNING
    private var firstTexture: String? = null
    private var currentFrame: FrameRecord? = null
    private var frameStartTick: Long = 0L

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
     * If every learned frame shares the same tick duration, returns that value; otherwise null.
     * Useful for outputting into the `AnimatedSkinJson` format which has a single `ticks` field.
     */
    val uniformTicks: Int?
        get() {
            if (_frames.isEmpty()) return null
            val first = _frames.first().ticks
            return if (_frames.all { it.ticks == first }) first else null
        }

    /**
     * Record the current skull texture for this server tick.
     *
     * @param tick  The tick number from [at.hannibal2.skyhanni.events.minecraft.ServerTickEvent].
     * @param frame The current skull frame, or null if no skull is present.
     * @return true if a loop was just completed.
     */
    fun record(tick: Long, frame: FrameRecord?): Boolean {
        if (frame == null) return false

        if (currentFrame == null) {
            // First frame ever seen, begin tracking.
            firstTexture = frame.fullTexture
            currentFrame = frame
            frameStartTick = tick
            return false
        }

        if (frame.fullTexture == currentFrame!!.fullTexture) return false

        // Texture changed, finalize the frame that just ended.
        val elapsed = (tick - frameStartTick).toInt().coerceAtLeast(1)
        val loopCompleted = onFrameEnd(currentFrame!!, elapsed)
        currentFrame = frame
        frameStartTick = tick
        return loopCompleted
    }

    private fun onFrameEnd(frame: FrameRecord, ticks: Int): Boolean = when (phase) {
        Phase.LEARNING -> {
            if (_frames.isNotEmpty() && frame.fullTexture == firstTexture) {
                // We've seen the first texture again, loop complete.
                // Verify that its tick duration matches what we learned initially.
                if (_frames.first().ticks != ticks) verificationErrors++
                loopCount++
                phase = Phase.VERIFYING
                // currentFrame is about to be set to the frame AFTER firstTexture,
                // so the next expected comparison is at index 1.
                verifyIndex = 1
                true
            } else {
                _frames.add(frame.copy(ticks = ticks))
                false
            }
        }

        Phase.VERIFYING -> {
            _frames.getOrNull(verifyIndex)?.let { expected ->
                if (expected.fullTexture != frame.fullTexture || expected.ticks != ticks) verificationErrors++
            }
            verifyIndex = (verifyIndex + 1) % _frames.size
            if (verifyIndex == 0) {
                loopCount++
                true
            } else false
        }
    }
}
