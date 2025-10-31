package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.canBeSeen
import at.hannibal2.hanni.utils.LocationUtils.distanceTo
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.MobUtils.isCompletelyDefault
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.particle.ParticleTypes

@HanniModule
object InvisibugHighlighter {
    private val config get() = HanniMod.feature.hunting.mobHighlight.invisibug

    private val invisibugEntities = mutableListOf<LivingEntity>()
    private var locationsToRender = setOf<LorenzVec>()

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

    @HandleEvent(HanniTickEvent::class, onlyOnIsland = IslandType.GALATEA)
    fun onTick() {
        if (!config.enabled) return
        locationsToRender = invisibugEntities.filter { it.canBeSeen(32) }.map { it.getLorenzVec() }.toSet()
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.enabled) return

        for (location in locationsToRender) {
            event.drawWaypointFilled(
                location - LorenzVec(0.4, -0.2, 0.4),
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
