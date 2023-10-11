package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class BlazeSlayerFirePitsWarning {
    private val config get() = SkyHanniMod.feature.slayer

    companion object {
        private var lastFirePitsWarning = 0L

        fun fireFirePits() {
            TitleUtils.sendTitle("§cFire Pits!", 2.seconds)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        val difference = System.currentTimeMillis() - lastFirePitsWarning

        if (difference in 1..2_000 && event.isMod(10) && config.firePitsWarning) {
            SoundUtils.createSound("random.orb", 0.8f).playSound()
        }
    }

    @SubscribeEvent
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
        LorenzUtils.inSkyBlock && config.firePitsWarning && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_3,
            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_3,
            BossType.SLAYER_BLAZE_QUAZII_4,
            BossType.SLAYER_BLAZE_TYPHOEUS_3,
            BossType.SLAYER_BLAZE_TYPHOEUS_4,
        )
}
