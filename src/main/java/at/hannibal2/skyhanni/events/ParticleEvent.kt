package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent.Cancellable
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries

/**
 * Shared particle packet payload used by the particle detection and handling events.
 *
 * The particle packet is exposed twice on purpose:
 * - [ParticleDetectedEvent] is the early, read-only signal that the packet was seen.
 *   Use it for detection, bookkeeping, and any logic that must not affect packet handling.
 * - [ParticleReceivedEvent] is the late, cancellable signal that fires right before the
 *   packet is processed normally.
 *   Use it when you want to hide or block the particle from spawning.
 *
 * Keeping these as separate events avoids mixing detection logic with cancellation logic
 * and makes it clear which listeners are safe to use for read-only tracking.
 *
 * @param type the particle type from the packet
 * @param location the particle spawn location from the packet
 * @param count how many particles the packet wants to spawn
 * @param speed the particle speed from the packet
 * @param offset the particle offsets from the packet
 * @param longDistance whether the packet bypasses the normal distance limiter
 * @param particleArgs optional particle-specific arguments, if present
 */
sealed class ParticleEvent(
    val type: ParticleType<*>,
    override val location: LorenzVec,
    val count: Int,
    val speed: Float,
    val offset: LorenzVec,
    val longDistance: Boolean,
    val particleArgs: IntArray? = null,
) : WorldEvent() {

    val distanceToPlayer by lazy { location.distanceToPlayer() }

    override fun toString(): String {
        return "${javaClass.simpleName}(type='${BuiltInRegistries.PARTICLE_TYPE.getKey(type)}', location=${location.roundTo(1)}, count=$count, speed=$speed, offset=$offset, longDistance=$longDistance, distanceToPlayer=${
            distanceToPlayer.roundTo(1)
        })"
    }
}

/**
 * Fired as soon as a particle packet is observed by the client packet listener.
 *
 * This is the early phase of particle handling. It is intentionally non-cancellable,
 * so listeners can detect particles without influencing whether the game processes them.
 *
 * Use this event for:
 * - tracking particle positions or patterns
 * - inferring hidden game state from packets
 * - any logic that should still run even if another listener later cancels the packet
 *
 * Do not use this event to hide particles or to depend on the packet actually spawning.
 */
class ParticleDetectedEvent(
    type: ParticleType<*>,
    location: LorenzVec,
    count: Int,
    speed: Float,
    offset: LorenzVec,
    longDistance: Boolean,
    particleArgs: IntArray? = null,
) : ParticleEvent(type, location, count, speed, offset, longDistance, particleArgs)

/**
 * Fired later in particle handling, right before the packet continues through the normal spawn path.
 *
 * This is the late phase of particle handling. It is cancellable and should be used when the
 * particle itself should be suppressed from spawning or rendering.
 *
 * Use this event for:
 * - hiding particles from the client
 * - preventing unwanted visual clutter
 * - any feature that must stop the packet from reaching the normal spawn path
 *
 * If you only need to detect or inspect the packet, prefer [ParticleDetectedEvent] instead.
 *
 * Listeners may call [cancel] to stop the particle packet from being processed.
 */
class ParticleReceivedEvent(
    type: ParticleType<*>,
    location: LorenzVec,
    count: Int,
    speed: Float,
    offset: LorenzVec,
    longDistance: Boolean,
    particleArgs: IntArray? = null,
) : ParticleEvent(type, location, count, speed, offset, longDistance, particleArgs), Cancellable


