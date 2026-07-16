package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.particles.ParticleTypes

@SkyHanniModule
object FlamingFlayHider {
    private enum class ParticleType(val check: ParticleEvent.() -> Boolean) {
        FLAY_1({ offset.x == 1.0 && offset.y == 1.0 && offset.z == 0.0 }),
        FLAY_2({ offset.x == 1.0 && offset.y == 0.0 && offset.z == 0.0 }),
        SOUL_WHIP_1({ offset.x == 0.23137255012989044 && offset.y == 0.05098039284348488 && offset.z == 0.0313725508749485 }),
        SOUL_WHIP_2({ offset.x == 0.003921568859368563 && offset.y == 0.003921568859368563 && offset.z == 0.003921568859368563 }),
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onParticle(event: ParticleEvent) {
        if (event.type != ParticleTypes.DUST) return
        if (event.distanceToPlayer > SkyHanniMod.feature.fishing.flayHideDistance) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return
        if (!ParticleType.entries.any { it.check(event) }) return
        event.cancel()
    }

}
