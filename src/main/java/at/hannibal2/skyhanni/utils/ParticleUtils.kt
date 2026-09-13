package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.Identifier

object ParticleUtils {
    private val cancelled = ThreadLocal.withInitial { false }

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
                speed = packet.maxSpeed,
                offset = packet.toOffset(),
                longDistance = packet.isOverrideLimiter,
            ).post().isCancelled
        ) {
            cancelled.set(true)
        }
    }

    @JvmStatic
    fun wrapAddParticle(
        level: ClientLevel,
        particleOptions: ParticleOptions,
        overrideLimiter: Boolean,
        alwaysShow: Boolean,
        x: Double,
        y: Double,
        z: Double,
        xd: Double,
        yd: Double,
        zd: Double,
        // Required for Java interop with Operation<Void>
        @Suppress("ForbiddenVoid")
        original: Operation<Void>,
        packet: ClientboundLevelParticlesPacket,
    ) {
        if (shouldSuppressParticle()) return

        val event = ParticleChangeEvent(particleOptions, packet)
        event.post()

        original.call(level, event.particleOptions, overrideLimiter, alwaysShow, x, y, z, xd, yd, zd)
    }

    private fun shouldSuppressParticle(): Boolean {
        val wasCancelled = cancelled.get()
        cancelled.set(false)
        return wasCancelled
    }
}
