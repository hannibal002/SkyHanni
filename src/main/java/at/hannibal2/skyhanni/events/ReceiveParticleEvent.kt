package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.VectorUtils.roundTo
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onReceiveParticle")
class ReceiveParticleEvent(
    val type: ParticleType<*>,
    override val location: Vec3,
    val count: Int,
    val speed: Float,
    val offset: Vec3,
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
