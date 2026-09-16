package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

/**
 * Fired when a particle is about to be added to the world, allowing the particle type to be replaced.
 *
 * Modifying [particleOptions] changes which particle will actually be spawned.
 *
 * @param particleOptions The particle type to be spawned. Can be replaced to change the visual.
 * @param packet The original packet that triggered the particle spawn.
 */
class ParticleChangeEvent(var particleOptions: ParticleOptions, val packet: ClientboundLevelParticlesPacket) : SkyHanniEvent()
