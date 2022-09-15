package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangHideParticles {

    @SubscribeEvent
    fun onReceivePacket(event: PlayParticleEvent) {
        if (LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.hideParticles &&
            DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
        ) {
            event.isCanceled = true
        }
    }
}