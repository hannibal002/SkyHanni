package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries

/**
 * Fired when a particle packet is received from the server.
 *
 * Unlike vanilla packet cancellation, canceling this event does not stop other
 * mods from seeing the packet. Instead, SkyHanni records the cancellation and
 * suppresses the particle later in the packet handler, after other mixins have
 * had a chance to process it.
 *
 * Runs on the network thread.
 * Will only fire if the player is in the world
 *
 * @param type the particle type
 * @param location the particle spawn location
 * @param count number of particles to spawn
 * @param xSpeed particle speed on the X axis
 * @param ySpeed particle speed on the Y axis
 * @param zSpeed particle speed on the Z axis
 * @param offset particle spawn offset
 * @param longDistance whether the packet bypasses normal distance checks
 * @param particleArgs optional particle-specific arguments
 */
@PrimaryFunction("onParticle")
class ParticleEvent(
    val type: ParticleType<*>,
    override val location: LorenzVec,
    val count: Int,
    val xSpeed: Float,
    val ySpeed: Float,
    val zSpeed: Float,
    val offset: LorenzVec,
    val longDistance: Boolean,
    val particleArgs: IntArray? = null,
) : CancellableWorldEvent() {
    val distanceToPlayer by lazy { location.distanceToPlayer() }

    /**
     * Checks whether the particle's [xSpeed], [ySpeed], and [zSpeed] all equal [speed].
     */
    fun isSpeed(speed: Float): Boolean = xSpeed == speed && ySpeed == speed && zSpeed == speed

    override fun toString(): String {
        return "${javaClass.simpleName}(type='${BuiltInRegistries.PARTICLE_TYPE.getKey(type)}', location=${location.roundTo(1)}, count=$count, xSpeed=$xSpeed, ySpeed=$ySpeed, zSpeed=$zSpeed, offset=$offset, longDistance=$longDistance, distanceToPlayer=${
            distanceToPlayer.roundTo(1)
        })"
    }
}
