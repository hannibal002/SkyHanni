package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BlazeSlayerClearView {

    private val hiddenFireBalls = mutableListOf<EntityFireball>()

    @SubscribeEvent
    fun onChatPacket(event: PlayParticleEvent) {
        if (!isEnabled()) return

        when (event.type) {
            EnumParticleTypes.SPELL_MOB,
            EnumParticleTypes.REDSTONE,
            EnumParticleTypes.FLAME,
            -> {
            }

            else -> return
        }

        val bossLocations = DamageIndicatorManager.getBosses()
            .filter { isBlazeBoss(it.bossType) }
            .map { it.entity.getLorenzVec() }
        val location = event.location
        if (bossLocations.any { it.distance(location) < 3 }) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity !is EntityFireball) return

        if (entity in hiddenFireBalls) {
            event.isCanceled = true
            return
        }

        val bossLocations = DamageIndicatorManager.getBosses()
            .filter { isBlazeBoss(it.bossType) }
            .map { it.entity.getLorenzVec() }

        val location = entity.getLorenzVec()
        if (bossLocations.any { it.distance(location) < 5 }) {
            hiddenFireBalls.add(entity)
            event.isCanceled = true
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.slayer.blazeClearView
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        hiddenFireBalls.clear()
    }

    private fun isBlazeBoss(type: BossType): Boolean {
        return when (type) {
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
            -> true

            else -> false
        }
    }
}