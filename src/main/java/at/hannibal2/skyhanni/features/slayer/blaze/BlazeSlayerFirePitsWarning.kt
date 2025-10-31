package at.hannibal2.hanni.features.slayer.blaze

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.BossHealthChangeEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.combat.damageindicator.BossType
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@HanniModule
object BlazeSlayerFirePitsWarning {

    private val config get() = SlayerApi.config.blazes

    private var lastFirePitsWarning = SimpleTimeMark.farPast()

    private fun fireFirePits() {
        TitleManager.sendTitle("§cFire Pits!", duration = 2.seconds)
        lastFirePitsWarning = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(10)) return

        if (lastFirePitsWarning.passedSince() < 2.seconds) {
            SoundUtils.createSound("random.orb", 0.8f).playSound()
        }
    }

    @HandleEvent
    fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (!isEnabled()) return
        val entityData = event.entityData

        val health = event.health
        val maxHealth = event.maxHealth
        val lastHealth = event.lastHealth

        val percentHealth = maxHealth * 0.33
        if (health < percentHealth && lastHealth > percentHealth) {
            when (entityData.bossType) {
                BossType.SLAYER_BLAZE_3,
                BossType.SLAYER_BLAZE_4,
                -> {
                    fireFirePits()
                }

                else -> {}
            }
        }
    }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && config.firePitsWarning && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_3,
            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_3,
            BossType.SLAYER_BLAZE_QUAZII_4,
            BossType.SLAYER_BLAZE_TYPHOEUS_3,
            BossType.SLAYER_BLAZE_TYPHOEUS_4,
        )

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.firePitsWarning", "slayer.blazes.firePitsWarning")
    }
}
