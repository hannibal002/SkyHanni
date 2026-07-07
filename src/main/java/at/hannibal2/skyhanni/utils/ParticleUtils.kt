package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.ParticleDetectedEvent
import at.hannibal2.skyhanni.events.ParticleReceivedEvent
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.Identifier

object ParticleUtils {
    fun getParticleTypeByName(name: String): Identifier = Identifier.parse(name.lowercase())

    @JvmStatic
    fun postReceivedParticleEvent(packet: ClientboundLevelParticlesPacket): Boolean {
        return ParticleReceivedEvent(
            type = packet.particle.type,
            location = packet.toLorenzVec(),
            count = packet.count,
            speed = packet.maxSpeed,
            offset = LorenzVec(packet.xDist, packet.yDist, packet.zDist),
            longDistance = packet.isOverrideLimiter,
        ).post()
    }

    @JvmStatic
    fun postDetectedParticleEvent(packet: ClientboundLevelParticlesPacket) {
        ParticleDetectedEvent(
            type = packet.particle.type,
            location = packet.toLorenzVec(),
            count = packet.count,
            speed = packet.maxSpeed,
            offset = LorenzVec(packet.xDist, packet.yDist, packet.zDist),
            longDistance = packet.isOverrideLimiter,
        ).post()
    }
}
