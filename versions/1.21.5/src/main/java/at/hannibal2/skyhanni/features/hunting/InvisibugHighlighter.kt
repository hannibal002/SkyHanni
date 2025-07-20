package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.isCompletelyDefault
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.particle.ParticleTypes

@SkyHanniModule
object InvisibugHighlighter {
    private val config get() = SkyHanniMod.feature.hunting.mobHighlight.invisibug

    private val invisibugEntities = mutableListOf<LivingEntity>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onParticle(event: ReceiveParticleEvent) {
        if (!config.enabled) return

        val particle = event.type
        if (particle != ParticleTypes.CRIT) return

        val nearestArmorStand = EntityUtils.getEntitiesNearby<ArmorStandEntity>(event.location, 5.0)
            .minByOrNull { it.distanceTo(event.location) }

        if (nearestArmorStand == null || !nearestArmorStand.isCompletelyDefault()) return

        invisibugEntities.add(nearestArmorStand)
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

        for (entity in invisibugEntities.toList()) {
            if (!entity.canBeSeen(32)) continue

            event.drawWaypointFilled(
                entity.getLorenzVec() - LorenzVec(0.4, -0.2, 0.4),
                config.color.toColor(),
                extraSize = -0.2,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled) return

        EntityUtils.removeInvalidEntities(invisibugEntities)
    }

    @HandleEvent
    fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(100, "foraging.mobHighlight.invisibug", "hunting.mobHighlight.invisibug")
    }
}
