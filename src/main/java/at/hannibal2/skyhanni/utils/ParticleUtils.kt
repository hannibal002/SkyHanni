package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.ParticleDetectedEvent
import at.hannibal2.skyhanni.events.ParticleReceivedEvent
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.Identifier

object ParticleUtils {
    private data class ParticlePacketData(
        val type: ParticleType<*>,
        val location: LorenzVec,
        val count: Int,
        val speed: Float,
        val offset: LorenzVec,
        val longDistance: Boolean,
    )

    fun getParticleTypeByName(name: String, shouldError: Boolean = false): Identifier = Identifier.parse(name.lowercase())

    @JvmStatic
    fun postReceivedParticleEvent(packet: ClientboundLevelParticlesPacket): Boolean {
        val data = packet.toParticlePacketData()
        val event = ParticleReceivedEvent(data.type, data.location, data.count, data.speed, data.offset, data.longDistance)
        return event.post()
    }

    @JvmStatic
    fun postDetectedParticleEvent(packet: ClientboundLevelParticlesPacket): Boolean {
        val data = packet.toParticlePacketData()
        val event = ParticleDetectedEvent(data.type, data.location, data.count, data.speed, data.offset, data.longDistance)
        return event.post()
    }

    private fun ClientboundLevelParticlesPacket.toParticlePacketData() = ParticlePacketData(
        type = particle.type,
        location = toLorenzVec(),
        count = count,
        speed = maxSpeed,
        offset = LorenzVec(xDist, yDist, zDist),
        longDistance = isOverrideLimiter,
    )

    // test
}
