package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

class ParticleChangeEvent(var particleOptions: ParticleOptions, val packet: ClientboundLevelParticlesPacket) : SkyHanniEvent()
