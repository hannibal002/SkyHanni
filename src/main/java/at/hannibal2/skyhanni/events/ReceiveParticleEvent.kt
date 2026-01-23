package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries

class ReceiveParticleEvent(
    val type: ParticleType<*>,
    override val location: LorenzVec,
    val count: Int,
    val speed: Float,
    val offset: LorenzVec,
    private val longDistance: Boolean,
    private val particleArgs: IntArray? = null,
) : CancellableWorldEvent() {

    val distanceToPlayer by lazy { location.distanceToPlayer() }

    override fun toString(): String {
        return "ReceiveParticleEvent(type='${BuiltInRegistries.PARTICLE_TYPE.getKey(type)}', location=${location.roundTo(1)}, count=$count, speed=$speed, offset=$offset, longDistance=$longDistance, distanceToPlayer=${
            distanceToPlayer.roundTo(1)
        })"
    }
}
