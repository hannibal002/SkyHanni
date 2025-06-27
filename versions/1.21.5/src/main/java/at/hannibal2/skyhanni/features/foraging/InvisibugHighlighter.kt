package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.particle.ParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InvisibugHighlighter {
    val config get() = SkyHanniMod.feature.foraging.foragingMobHighlight.invisibugHighlight

    private fun isEnabled() = config.enabled && isInIsland()
    private fun isInIsland() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny()

    private val particlesToHighlight = mutableMapOf<LorenzVec, SimpleTimeMark>()
    private val invisibugEntities = mutableSetOf<LivingEntity>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onParticle(event: ReceiveParticleEvent) {
        val particle = event.type
        if (particle != ParticleTypes.CRIT) return

        val nearestArmorStand = EntityUtils.getAllEntities()
            .filterIsInstance<ArmorStandEntity>()
            .minByOrNull { it.distanceTo(event.location) }

        if (nearestArmorStand == null || nearestArmorStand.distanceTo(event.location) > 5) return

        invisibugEntities.add(nearestArmorStand)
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        invisibugEntities.removeIf() {
            !EntityUtils.getAllEntities().filterIsInstance<ArmorStandEntity>().contains(it)
        }

        for (entity in invisibugEntities) {
            if (entity.isDead || entity.distanceToPlayer() > 20) continue

            event.drawWaypointFilled(
                entity.getLorenzVec(),
                config.color.toColor(),
                extraSize = -0.2
            )
        }
    }
}
