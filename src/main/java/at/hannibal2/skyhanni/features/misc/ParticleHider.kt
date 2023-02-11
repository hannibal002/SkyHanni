package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ParticleHider {

    @SubscribeEvent
    fun onHypExplosions(event: ReceiveParticleEvent) {
        val distance = event.location.distance(LocationUtils.playerLocation())
        if (SkyHanniMod.feature.misc.hideFarParticles) {
            if (distance > 40) {
                event.isCanceled = true
                return
            }
        }

        val type = event.type
        if (SkyHanniMod.feature.misc.hideCloseRedstoneparticles) {
            if (type == EnumParticleTypes.REDSTONE) {
                if (distance < 2) {
                    event.isCanceled = true
                    return
                }
            }
        }

        if (SkyHanniMod.feature.misc.hideFireballParticles) {
            if (type == EnumParticleTypes.SMOKE_NORMAL || type == EnumParticleTypes.SMOKE_LARGE) {
                event.isCanceled = true
                return
            }
        }
    }
}