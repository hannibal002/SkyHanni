package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.monster.EnderMan

@SkyHanniModule
object EndermanSlayerHideParticles {

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.config.enderman.hideParticles) return

        when (event.type) {
            ParticleTypes.LARGE_SMOKE,
            ParticleTypes.FLAME,
            ParticleTypes.WITCH,
            -> Unit

            else -> return
        }

        if (EntityUtils.getEntitiesInBoundingBox<EnderMan>(event.location.boundingCenter(3.0)).isNotEmpty()) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.endermanHideParticles", "slayer.endermen.hideParticles")
    }
}
