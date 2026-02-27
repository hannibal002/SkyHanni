package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawCircleWireframe
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactBoundingBox
import net.minecraft.core.Rotations
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FireFreezeFeatures {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.fireFreeze
    private const val PARTICLE_OFFSET = 3.921568568330258E-4

    private data class FireFreezeArea(
        val center: LorenzVec,
        val lastPitch: Float,
        var startTime: ServerTimeMark = timeFromPitch(lastPitch),
        var knownTime: Boolean = false,
        var frozen: Boolean = false,
    ) {
        fun update(pitch: Float) {
            if (knownTime || lastPitch == pitch) return
            startTime = timeFromPitch(pitch)
            knownTime = true
        }

        fun hasFinished(): Boolean = frozen || startTime.passedSince() > 0.5.seconds
        // 0.5s passed since is for times where the fire freeze misses and hypixel plays no noise.

        fun freezeMobs() {
            if (frozen) return
            frozen = true
            for (mob in MobData.skyblockMobs) {
                if (isInside(pos = mob.baseEntity.getLorenzVec(), extra = 0.0)) freezeMob(mob)
            }
        }

        fun isInside(pos: LorenzVec, extra: Double = 0.5): Boolean =
            center.distanceIgnoreY(pos) < (RADIUS + extra) // add extra for possibly loose particles
    }

    private fun freezeMob(mob: Mob) {
        val prevTime = affectedMobs[mob]
        if (prevTime == null || prevTime.isInPast()) {
            affectedMobs[mob] = ServerTimeMark.now() + freezeDuration
        }
    }

    private val ARMORSTAND_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("FIRE_FREEZE_SKULLS") }

    private val affectedMobs = ConcurrentHashMap<Mob, ServerTimeMark>()
    private val fireFreezes = ConcurrentHashMap<LorenzVec, FireFreezeArea>()

    private const val RADIUS = 5.0
    private val freezeDuration = 10.seconds

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        when (event.soundName) {
            "mob.guardian.elder.idle" -> handleActiveSound(event)
            "random.anvil_land" -> handleAnvilSound(event)
        }
    }

    private fun handleActiveSound(event: PlaySoundEvent) {
        if (event.volume != 0.2f) return
        val pos = event.location
        val pitch = event.pitch
        if (pitch !in 0.0..2.0) return
        val fireFreeze = fireFreezes[pos]
        if (fireFreeze == null) {
            fireFreezes[pos] = FireFreezeArea(pos, pitch)
            return
        }
        fireFreeze.update(pitch)
    }

    private fun handleAnvilSound(event: PlaySoundEvent) {
        if (event.volume != 0.6f) return
        if (event.pitch != 0.4920635f) return
        val fireFreeze = fireFreezes[event.location] ?: return
        fireFreeze.freezeMobs()
        fireFreezes.remove(event.location)
    }

    private fun LorenzVec.isInAnyFireFreeze(): Boolean = fireFreezes.values.any { !it.hasFinished() && it.isInside(this) }

    private fun ReceiveParticleEvent.isFreezeParticle(): Boolean {
        return offset.x == PARTICLE_OFFSET && offset.y == PARTICLE_OFFSET && offset.z == PARTICLE_OFFSET
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onParticle(event: ReceiveParticleEvent) {
        if (event.type != ParticleTypes.DUST) return
        if (event.count != 0 || event.speed != 1.0f || !event.isFreezeParticle()) return
        if (!config.customCircle) return
        if (event.location.isInAnyFireFreeze()) event.cancel()
    }

    private fun ArmorStand.isFireFreeze(): Boolean {
        if (!isInvisible) return false

        if (!headPose.isZero() || !bodyPose.isZero()) return false
        val texture = getStandHelmet()?.getSkullTexture() ?: return false
        return texture == ARMORSTAND_SKULL_TEXTURE
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGH)
    fun onRenderLiving(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!config.customCircle) return
        if (event.entity.isFireFreeze()) event.cancel()
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) = affectedMobs.remove(event.mob)

    @HandleEvent
    fun onWorldChange() {
        affectedMobs.clear()
        fireFreezes.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        fireFreezes.values.removeIf { it.frozen || it.startTime.passedSince() > 2.seconds }
        affectedMobs.removeIf { (mob, time) -> !mob.isAlive || time.isInPast() }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (config.customCircle) event.renderCustomCircle()
        if (config.freezeTimer) event.renderCircleTimer()
        if (config.mobHighlight || config.mobTimer) event.renderMobs()
    }

    private fun SkyHanniRenderWorldEvent.renderCustomCircle() {
        for (fireFreeze in fireFreezes.values) {
            if (fireFreeze.hasFinished()) continue
            drawCircleWireframe(fireFreeze.center.up(1), RADIUS, config.displayColor.toColor())
        }
    }

    private fun SkyHanniRenderWorldEvent.renderCircleTimer() {
        for (fireFreeze in fireFreezes.values) {
            if (fireFreeze.hasFinished()) continue
            drawDynamicText(
                location = fireFreeze.center.up(1),
                text = "§b❄ ${LorenzColor.AQUA.getChatColor()}${fireFreeze.startTime.timeUntil().formatTime()}",
                scaleMultiplier = 1.0,
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.renderMobs() {
        for ((mob, time) in affectedMobs) {
            val timeUntil = time.timeUntil()
            if (!mob.isAlive || timeUntil.isNegative()) continue

            val percent = 1 - ((timeUntil.inPartialSeconds - 5) / 5).coerceAtLeast(0.0).coerceAtMost(1.0)
            val color = ColorUtils.blendRGB(
                LorenzColor.YELLOW,
                LorenzColor.RED,
                percent,
            )
            val exactLocation = mob.baseEntity.getLorenzVec().add(-0.5, 0.0, -0.5)

            if (config.mobTimer) {
                val format = timeUntil.formatTime()
                val text = componentBuilder {
                    appendWithColor("❄ ", LorenzColor.AQUA.toColor().rgb)
                    appendWithColor(format, color.rgb)
                }
                drawDynamicText(
                    location = exactLocation,
                    text = text,
                    scaleMultiplier = 1.0,
                )
            }
            if (config.mobHighlight) {
                val aabb = exactBoundingBox(mob.baseEntity).expand(0.1)
                drawFilledBoundingBox(aabb, color, 0.5f)
            }
        }
    }

    private fun Duration.formatTime() = format(showMilliSeconds = true)

    // Starts at 2.0 pitch and goes down by 0.5 every 2 seconds, not going lower than 0.0. This lets us estimate
    // how long it will take for the fire freeze to take effect
    private fun timeFromPitch(pitch: Float): ServerTimeMark = ServerTimeMark.now() + (2.0 * pitch + 1).seconds

    private fun Rotations.isZero(): Boolean = x == 0.0f && y == 0.0f && z == 0.0f

}
