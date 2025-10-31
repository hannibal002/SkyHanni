package at.hannibal2.hanni.features.slayer.blaze

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.combat.damageindicator.BossType
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.entity.projectile.EntityFireball

@HanniModule
object BlazeSlayerClearView {

    private var nearBlaze = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(3)) return
        nearBlaze = DamageIndicatorManager.getDistanceTo(
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (isEnabled()) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityFireball>) {
        if (isEnabled()) {
            event.cancel()
        }
    }

    private fun isEnabled() = SlayerApi.config.blazes.clearView && nearBlaze

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.blazeClearView", "slayer.blazes.clearView")
    }
}
