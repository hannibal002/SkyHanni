package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.events.SpawnParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class AshfangHideParticles {

    var tick = 0
    var hideParticles = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return

        if (tick++ % 60 == 0) {
            val distance = DamageIndicatorManager.getDistanceTo(BossType.NETHER_ASHFANG)
            hideParticles = distance < 40
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: PlayParticleEvent) {
        if (!isEnabled()) return
        if (!hideParticles) return

        event.isCanceled = true
    }

    @SubscribeEvent
    fun onSpawnParticle(event: SpawnParticleEvent) {
        if (!isEnabled()) return
        if (!hideParticles) return

        event.isCanceled = true
    }

    private fun isEnabled() = LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.hideParticles
}