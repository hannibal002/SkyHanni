package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.ParticleDetectedEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.ParticleReceivedEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.Identifier

object ParticleUtils {
    fun getParticleTypeByName(name: String): Identifier? {
        val id = Identifier.tryParse(name) ?: return null
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
            return null
        }
        return id
    }

    @JvmStatic
    fun postParticleEvent(packet: ClientboundLevelParticlesPacket): Boolean {
        return ParticleEvent(
            type = packet.particle.type,
            location = packet.toLorenzVec(),
            count = packet.count,
            speed = packet.maxSpeed,
            offset = packet.toOffset(),
            longDistance = packet.isOverrideLimiter,
        ).post()
    }
}
