package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.projectile.EntitySmallFireball
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ParticleHider {

    private fun inM7Boss() = LorenzUtils.inDungeons && DungeonAPI.dungeonFloor == "M7" && DungeonAPI.inBossRoom

    @SubscribeEvent
    fun onHypExplosions(event: ReceiveParticleEvent) {
        val distanceToPlayer = event.distanceToPlayer
        if (SkyHanniMod.feature.misc.hideFarParticles) {
            if (distanceToPlayer > 40 && !inM7Boss()) {
                event.isCanceled = true
                return
            }
        }

        val type = event.type
        if (SkyHanniMod.feature.misc.hideCloseRedstoneparticles) {
            if (type == EnumParticleTypes.REDSTONE) {
                if (distanceToPlayer < 2) {
                    event.isCanceled = true
                    return
                }
            }
        }

        if (SkyHanniMod.feature.misc.hideFireballParticles) {
            if (type == EnumParticleTypes.SMOKE_NORMAL || type == EnumParticleTypes.SMOKE_LARGE) {
                for (entity in EntityUtils.getEntities<EntitySmallFireball>()) {
                    val distance = entity.getLorenzVec().distance(event.location)
                    if (distance < 5) {
                        event.isCanceled = true
                        return
                    }
                }
            }
        }
    }
}