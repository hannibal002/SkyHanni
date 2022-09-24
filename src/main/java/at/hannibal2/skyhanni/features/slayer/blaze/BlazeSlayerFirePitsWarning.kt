package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BlazeSlayerFirePitsWarning {

    companion object {
        private var lastFirePitsWarning = 0L
        private var nextTickIn = 0

        fun fireFirePits() {
            SendTitleHelper.sendTitle("§cFire Pits!", 2_000)
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val difference = System.currentTimeMillis() - lastFirePitsWarning

        if (difference > 0) {
            if (difference <= 2_000) {
                if (nextTickIn++ % 10 == 0) {
                    if (SkyHanniMod.feature.slayer.firePitsWarning) {
                        SoundUtils.createSound("random.orb", 0.8f).playSound()
                    }
                }
            }
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
        if (health < percentHealth) {
            if (lastHealth > percentHealth) {
                when (entityData.bossType) {
                    BossType.SLAYER_BLAZE_3,
                        //TODO blaze slayer tier 4
                        //BossType.SLAYER_BLAZE_4,
                    -> {
                        fireFirePits()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_3,
            //TODO blaze slayer tier 4
//            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_34,
            BossType.SLAYER_BLAZE_TYPHOEUS_34,
        )
    }
}