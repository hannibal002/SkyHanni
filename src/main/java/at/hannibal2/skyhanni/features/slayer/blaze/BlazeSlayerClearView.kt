package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.events.SpawnParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.projectile.EntityFireball
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BlazeSlayerClearView {

    var tick = 0
    var hideParticles = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (tick++ % 60 == 0) {
            hideParticles = DamageIndicatorManager.getDistanceTo(
                BossType.SLAYER_BLAZE_1,
                BossType.SLAYER_BLAZE_2,
                BossType.SLAYER_BLAZE_3,
                BossType.SLAYER_BLAZE_4,
                BossType.SLAYER_BLAZE_TYPHOEUS_1,
                BossType.SLAYER_BLAZE_TYPHOEUS_2,
                BossType.SLAYER_BLAZE_TYPHOEUS_3,
                BossType.SLAYER_BLAZE_TYPHOEUS_4,
                BossType.SLAYER_BLAZE_QUAZII_1,
                BossType.SLAYER_BLAZE_QUAZII_2,
                BossType.SLAYER_BLAZE_QUAZII_3,
                BossType.SLAYER_BLAZE_QUAZII_4,
            ) < 10
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: PlayParticleEvent) {
        if (isEnabled()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onSpawnParticle(event: SpawnParticleEvent) {
        if (isEnabled()) {
            when (event.callerClass) {
                "net.minecraft.block.BlockFire",
                "net.minecraft.entity.monster.EntityBlaze",
                "net.minecraft.entity.projectile.EntityFireball",
                -> {
                    event.isCanceled = true
                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (isEnabled()) {
            if (event.entity is EntityFireball) {
                event.isCanceled = true
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.slayer.blazeClearView && hideParticles
    }
}