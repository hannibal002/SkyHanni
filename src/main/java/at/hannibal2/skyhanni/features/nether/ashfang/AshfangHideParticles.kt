package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangHideParticles {

    @SubscribeEvent
    fun onReceivePacket(event: PacketEvent.ReceiveEvent) {
        if (event.packet !is S2APacketParticles) return

        if (isEnabled()) {
            event.isCanceled = true
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.hideParticles &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}