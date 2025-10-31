package at.hannibal2.hanni.features.slayer.enderman

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.getLorenzVec
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.util.EnumParticleTypes

@HanniModule
object EndermanSlayerHideParticles {

    private var endermanLocations = listOf<LorenzVec>()

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        endermanLocations = EntityUtils.getEntities<EntityEnderman>().map { it.getLorenzVec() }.toList()
    }

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        when (event.type) {
            EnumParticleTypes.SMOKE_LARGE,
            EnumParticleTypes.FLAME,
            EnumParticleTypes.SPELL_WITCH,
            -> Unit

            else -> return
        }

        val distance = event.location.distanceToNearestEnderman() ?: return
        if (distance < 9) {
            event.cancel()
        }
    }

    private fun LorenzVec.distanceToNearestEnderman() = endermanLocations.minOfOrNull { it.distanceSq(this) }

    fun isEnabled() = IslandType.THE_END.isCurrent() && SlayerApi.config.endermen.hideParticles

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.endermanHideParticles", "slayer.endermen.hideParticles")
    }
}
