package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent.Cancellable
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries

/**
 * Base class for particle events, fired in two phases to support mod interoperability.
 *
 * The particle packet triggers two separate events ([ParticleDetectedEvent] and [ParticleReceivedEvent])
 * so that detection features always see particles, even if other mods or features later hide them.
 *
 * @param type the particle type from the packet
 * @param location the particle spawn location
 * @param count number of particles to spawn
 * @param speed particle speed
 * @param offset particle spawn offset
 * @param longDistance whether the packet bypasses normal distance checks
 * @param particleArgs optional particle-specific arguments
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
 * Fired as soon as a particle packet is observed by the client.
 *
 * This early, read-only phase ensures detection features see all particles,
 * even if other mods or features later hide them.
 * Do not use this event to hide particles; use [ParticleReceivedEvent] instead.
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
 * Fired right before a particle packet is processed by the client.
 *
 * This late, cancellable phase is used to hide particles without affecting other mods'
 * particle detection. Cancelling this event prevents the particle from spawning.
 * For detection logic, use [ParticleDetectedEvent] instead.
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
