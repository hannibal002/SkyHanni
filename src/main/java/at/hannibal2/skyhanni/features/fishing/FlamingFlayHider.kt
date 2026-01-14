package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.particles.ParticleTypes

@SkyHanniModule
object FlamingFlayHider {
    private enum class ParticleType(val check: ReceiveParticleEvent.() -> Boolean) {
        FLAY_1({ offset.x == 1.0 && offset.y == 1.0 && offset.z == 0.0 }),
        FLAY_2({ offset.x == 1.0 && offset.y == 0.0 && offset.z == 0.0 }),
        SOUL_WHIP_1({ offset.x == 0.2 && offset.y == 0.1 && offset.z == 0.0 }),
        SOUL_WHIP_2({ offset.x == 0.0 && offset.y == 0.0 && offset.z == 0.0 }),
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(particle: ReceiveParticleEvent) {
        if (particle.type != ParticleTypes.DUST) return
        if (particle.distanceToPlayer > SkyHanniMod.feature.fishing.flayHideDistance) return
        if (particle.count != 0) return
        if (particle.speed != 1.0f) return
        if (!ParticleType.entries.any { it.check(particle) }) return
        particle.cancel()
    }

}
