package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
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
        if (SkyHanniMod.feature.misc.particleHider.hideFarParticles && distanceToPlayer > 40 && !inM7Boss()) {
            event.isCanceled = true
            return
        }

        val type = event.type
        if (SkyHanniMod.feature.misc.particleHider.hideCloseRedstoneParticles && type == EnumParticleTypes.REDSTONE && distanceToPlayer < 2) {
            event.isCanceled = true
            return
        }

        if (SkyHanniMod.feature.misc.particleHider.hideFireballParticles && (type == EnumParticleTypes.SMOKE_NORMAL || type == EnumParticleTypes.SMOKE_LARGE)) {
            for (entity in EntityUtils.getEntities<EntitySmallFireball>()) {
                val distance = entity.getLorenzVec().distance(event.location)
                if (distance < 5) {
                    event.isCanceled = true
                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(4,"misc.hideBlazeParticles", "misc.particleHider.hideBlazeParticles")
        event.move(4, "misc.hideEndermanParticles", "misc.particleHider.hideEndermanParticles")
        event.move(4, "misc.hideFarParticles", "misc.particleHider.hideFarParticles")
        event.move(4, "misc.hideFireballParticles", "misc.particleHider.hideFireballParticles")
        event.move(4, "misc.hideCloseRedstoneparticles", "misc.particleHider.hideCloseRedstoneParticles")
        event.move(4, "misc.hideFireBlockParticles", "misc.particleHider.hideFireBlockParticles")
        event.move(4,"misc.hideSmokeParticles", "misc.particleHider.hideSmokeParticles")

    }

}