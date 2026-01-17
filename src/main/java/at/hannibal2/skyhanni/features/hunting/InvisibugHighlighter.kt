package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.isCompletelyDefault
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object InvisibugHighlighter {
    private val config get() = SkyHanniMod.feature.hunting.mobHighlight.invisibug

    private const val DISTANCE = 5.0

    private val invisibugEntities = mutableSetOf<LivingEntity>()
    private var locationsToRender = listOf<LorenzVec>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onParticle(event: ReceiveParticleEvent) {
        if (!config.enabled) return

        val particle = event.type
        if (particle != ParticleTypes.CRIT) return
        if (invisibugEntities.any { it.distanceTo(event.location) < DISTANCE }) return

        val aabb = event.location.boundingCenter(DISTANCE)
        val nearestArmorStand = EntityUtils.getEntitiesInBoundingBox<ArmorStand>(aabb).minByOrNull { it.distanceTo(event.location) } ?: return

        if (!nearestArmorStand.isCompletelyDefault()) return

        DelayedRun.runOrNextTick { invisibugEntities.add(nearestArmorStand) }

    }

    private val renderOffset = LorenzVec(0.4, -0.2, 0.4)

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!config.enabled) return
        locationsToRender = invisibugEntities.mapNotNull {
            if (it.canBeSeen(32)) it.getLorenzVec() else null
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        if (locationsToRender.isEmpty()) return
        val color = config.color.toColor()

        for (location in locationsToRender) {
            event.drawWaypointFilled(
                location - renderOffset,
                color,
                extraSize = -0.2,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        invisibugEntities.remove(event.entity)
    }

    @HandleEvent
    fun onWorldChange() {
        invisibugEntities.clear()
    }

    @HandleEvent
    fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(100, "foraging.mobHighlight.invisibug", "hunting.mobHighlight.invisibug")
    }
}
