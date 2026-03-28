package at.hannibal2.skyhanni.features.rift.area.livingcave

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.EntityUtils.isAtFullHealth
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.VectorUtils.blockCenter
import at.hannibal2.skyhanni.utils.VectorUtils.up
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StainedGlassBlock
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LivingCaveDefenseBlocks {

    private val config get() = RiftApi.config.area.livingCave.defenseBlock

    private val movingBlocks = ConcurrentHashMap<DefenseBlock, SimpleTimeMark>()
    private val staticBlocks = ConcurrentHashMap.newKeySet<DefenseBlock>()

    data class DefenseBlock(
        val entity: RemotePlayer,
        val location: Vec3,
    ) {
        var hidden: Boolean = false
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return
        staticBlocks.removeIf { it.entity.deceased }
    }

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        movingBlocks.values.removeIf { it.passedSince() > 2.seconds }
        movingBlocks.keys.removeIf {
            staticBlocks.any { others -> others.location.distanceTo(it.location) < 1.5 }
        }

        val location = event.location.add(-0.5, 0.0, -0.5)

        // Ignore particles around blocks
        if (staticBlocks.any { it.location.distanceTo(location) < 3 }) {
            if (config.hideParticles) {
                event.cancel()
            }
            return
        }
        if (config.hideParticles && movingBlocks.keys.any { it.location.distanceTo(location) < 3 }) {
            event.cancel()
        }

        if (event.type == ParticleTypes.ENCHANTED_HIT) {
            var entity: RemotePlayer? = null

            // read old entity data
            getNearestMovingDefenseBlock(location)?.let {
                if (it.location.distanceTo(location) < 0.5) {
                    it.hidden = true
                    entity = it.entity
                }
            }

            if (entity == null) {
                // read new entity data
                val compareLocation = event.location.add(-0.5, -1.5, -0.5)
                entity = compareLocation.getEntitiesNearby<RemotePlayer>(2.0)
                    .filter { isCorrectMob(it.name.formattedTextCompatLessResets()) }
                    .filter { !it.isAtFullHealth() }
                    .minByOrNull { it.distanceTo(compareLocation) }
            }

            val defenseBlock = entity?.let { DefenseBlock(it, location) } ?: return

            movingBlocks[defenseBlock] = SimpleTimeMark.now() + 250.milliseconds
            if (config.hideParticles) {
                event.cancel()
            }
        }
    }

    private fun isCorrectMob(name: String) = when (name) {
        "Autonull ",

        "Autocap ",
        "Autochest ",
        "Autopants ",
        "Autoboots ",
        -> true

        else -> false
    }

    @HandleEvent
    fun onServerBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled()) return

        val location = event.location
        val old = event.oldState
        val new = event.newState

        // spawn block
        if (old.block == Blocks.AIR && (new.block is StainedGlassBlock || new.block == Blocks.DIAMOND_BLOCK)) {
            val entity = getNearestMovingDefenseBlock(location)?.entity ?: return
            staticBlocks.add(DefenseBlock(entity, location))
            staticBlocks.forEach { block ->
                RenderLivingEntityHelper.setEntityColor(
                    block.entity,
                    color.addAlpha(50),
                ) { isEnabled() && staticBlocks.any { it.entity == block.entity } }
            }
        }

        // despawn block
        val nearestBlock = getNearestStaticDefenseBlock(location)
        if (new.block == Blocks.AIR && location == nearestBlock?.location) {
            staticBlocks.remove(nearestBlock)
        }
    }

    private fun getNearestMovingDefenseBlock(location: Vec3) =
        movingBlocks.keys.filter { it.location.distanceTo(location) < 15 }
            .minByOrNull { it.location.distanceTo(location) }

    private fun getNearestStaticDefenseBlock(location: Vec3) =
        staticBlocks.filter { it.location.distanceTo(location) < 15 }
            .minByOrNull { it.location.distanceTo(location) }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((block, time) in movingBlocks) {
            if (block.hidden) continue
            if (time.isInFuture()) {
                val location = block.location
                event.drawWaypointFilled(location, color)
                event.drawLineToCrosshair(
                    location.blockCenter(),
                    color,
                    1,
                    false,
                )
            }
        }
        for (block in staticBlocks) {
            val location = block.location
            event.drawDynamicText(location, "§bBreak!", 1.5, seeThroughBlocks = false)
            event.drawWaypointFilled(location, color)

            event.draw3DLine(
                event.exactLocation(block.entity).up(0.5),
                location.blockCenter(),
                color,
                3,
                true,
            )
        }
    }

    private val color get() = config.color.get().toColor()

    fun isEnabled() = RiftApi.inRift() && config.enabled && RiftApi.inLivingCave()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.livingCaveConfig", "rift.area.livingCave")

        val basePath = "rift.area.livingCave"
        event.move(82, "$basePath.defenseBlockConfig", "$basePath.defenseBlock")
        event.move(82, "$basePath.livingCaveLivingMetalConfig", "$basePath.livingMetal")
    }
}
