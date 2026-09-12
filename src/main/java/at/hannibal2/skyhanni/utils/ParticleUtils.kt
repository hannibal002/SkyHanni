package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.Identifier

object ParticleUtils {
    fun getParticleTypeByName(name: String): Identifier? {
        val id = Identifier.tryParse(name.lowercase()) ?: return null
        if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
            return null
        }
        return id
    }

    @JvmStatic
    fun postParticleEvent(packet: ClientboundLevelParticlesPacket) {
        if (!MinecraftCompat.localPlayerExists) return
        cancelled.set(false)
        if (ParticleEvent(
                type = packet.particle.type,
                location = packet.toLorenzVec(),
                count = packet.count,
                //? if >= 26.3 {
                xSpeed = packet.xMaxSpeed,
                ySpeed = packet.yMaxSpeed,
                zSpeed = packet.zMaxSpeed,
                //?} else {
                /*xSpeed = packet.maxSpeed,
                ySpeed = packet.maxSpeed,
                zSpeed = packet.maxSpeed,
                *///?}
                offset = packet.toOffset(),
                //~ if < 26.3 'overrideLimiter' -> 'isOverrideLimiter'
                longDistance = packet.overrideLimiter(),
            ).post().isCancelled
        ) {
            cancelled.set(true)
        }
    }

    private val cancelled = ThreadLocal.withInitial { false }

    @JvmStatic
    fun shouldSuppressParticle(): Boolean {
        val wasCancelled = cancelled.get()
        cancelled.set(false)
        return wasCancelled
    }
}
