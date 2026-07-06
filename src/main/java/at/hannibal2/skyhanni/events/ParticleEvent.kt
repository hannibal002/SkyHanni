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
 * Use this for detection, tracking, and other read-only reactions.
 */
class ParticleDetectedEvent(
    type: ParticleType<*>,
    override val location: LorenzVec,
    count: Int,
    speed: Float,
    offset: LorenzVec,
    longDistance: Boolean,
    particleArgs: IntArray? = null,
) : ParticleEvent(type, location, count, speed, offset, longDistance, particleArgs)

/**
 * Fired later in particle handling, right before the packet continues through the normal spawn path.
 *
 * Listeners may call [cancel] to stop the particle packet from being processed.
 */
class ParticleReceivedEvent(
    type: ParticleType<*>,
    override val location: LorenzVec,
    count: Int,
    speed: Float,
    offset: LorenzVec,
    longDistance: Boolean,
    particleArgs: IntArray? = null,
) : ParticleEvent(type, location, count, speed, offset, longDistance, particleArgs), Cancellable


