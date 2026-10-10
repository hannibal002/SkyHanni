package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.DelayedRun.runDelayed
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.PlayerUtils.SNEAKING_EYE_HEIGHT
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object VampireSlayerFeatures {

    private val config get() = SlayerApi.config.vampire
    private val configBoss get() = config.boss
    private val configBloodIchor get() = config.bloodIchor
    private val configKillerSpring get() = config.killerSpring

    private enum class EffectType {
        BLOOD_ICHOR,
        KILLER_SPRING,
    }

    private data class TrackedBoss(
        val mob: Mob,
        val effects: MutableMap<ArmorStand, EffectType> = linkedMapOf(),
        var steakAlertSent: Boolean = false,
    ) {
        val entity get() = mob.baseEntity
        val armorStand get() = mob.armorStand
    }

    private val trackedBosses = mutableListOf<TrackedBoss>()

    private val BLOOD_ICHOR_TEXTURE by SkullTextureHolder.texture("BLOOD_ICHOR")
    private val KILLER_SPRING_TEXTURE by SkullTextureHolder.texture("KILLER_SPRING")

    private var lastWitherSpawnSound = ServerTimeMark.farPast()
    private var nextTwinClawsTitle = SimpleTimeMark.farPast()
    private var twinClawsTitlePending = false

    private val patternGroup = RepoPattern.group("slayer.vampire-features")

    /**
     * WRAPPED-REGEX-TEST: "Bloodfiend "
     */
    private val bossNamePattern by patternGroup.pattern(
        "boss-name",
        "Bloodfiend .*",
    )

    /**
     * WRAPPED-REGEX-TEST: "TWINCLAWS 1.2s"
     */
    private val twinClawsPattern by patternGroup.pattern(
        "twinclaws-name",
        ".*TWINCLAWS.*",
    )

    /**
     * REGEX-TEST: Bloodfiend III 312/1,800❤ ҉
     */
    private val steakHealthPattern by patternGroup.pattern(
        "steak-health",
        "Bloodfiend .* [\\d,]+/[\\d,]+❤ ҉",
    )

    private const val MAX_SLAYER_DISTANCE = 20.0

    private fun trackedBoss(entity: RemotePlayer): TrackedBoss? =
        trackedBosses.firstOrNull { it.entity === entity }

    private fun closestTrackedBoss(position: LorenzVec, range: Double): TrackedBoss? =
        trackedBosses
            .map { it to it.entity.distanceTo(position) }
            .filter { it.second <= range }
            .minByOrNull { it.second }
            ?.first

    private fun groupSpringStands(effects: Map<ArmorStand, EffectType>): List<List<ArmorStand>> {
        val groups = mutableListOf<MutableList<ArmorStand>>()
        for (stand in effects.filterValues { it == EffectType.KILLER_SPRING }.keys) {
            val position = stand.blockPosition()
            val group = groups.firstOrNull { members ->
                members.any {
                    val memberPosition = it.blockPosition()
                    abs(position.x - memberPosition.x) <= 1 &&
                        abs(position.z - memberPosition.z) <= 1
                }
            }
            if (group == null) {
                groups += mutableListOf(stand)
            } else {
                group += stand
            }
        }
        return groups
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        val removed = trackedBosses.filter { it.entity.distanceToPlayer() > MAX_SLAYER_DISTANCE }
        trackedBosses.removeAll(removed)
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.baseEntity !is RemotePlayer || !bossNamePattern.matches(mob.baseEntity.cleanName)) return
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        trackedBosses.removeIf { it.mob === event.mob }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEffectLeaveWorld(event: EntityLeaveWorldEvent<ArmorStand>) {
        trackedBosses.forEach { it.effects.remove(event.entity) }
    }

    private fun processBossSteak(boss: TrackedBoss, healthName: String?) {
        val entity = boss.entity as? RemotePlayer ?: return
        val canUseSteak = healthName?.let { steakHealthPattern.matches(it) } == true
        if (canUseSteak) {
            if (!boss.steakAlertSent && configBoss.steakAlert) {
                boss.steakAlertSent = true
                TitleManager.sendTitle("§c§lSTEAK!", duration = 300.milliseconds)
            }
        } else {
            boss.steakAlertSent = false
        }
        if (!configBoss.highlight) return
        val color = if (canUseSteak && config.changeColorWhenCanSteak) {
            config.steakColor.toColor()
        } else {
            configBoss.highlightColor.toColor()
        }
        RenderLivingEntityHelper.setEntityColor(entity, color) {
            isEnabled() && trackedBoss(entity) === boss
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled() || event.action != EntityClickEvent.ActionType.ATTACK) return
        val entity = event.clickedEntity as? RemotePlayer ?: return
        val cleanName = entity.cleanName
        val matches = bossNamePattern.matches(cleanName)
        if (!matches) return
        val mob = entity.mob
        if (mob == null || mob.baseEntity !== entity) return
        if (trackedBoss(entity) == null) {
            trackedBosses.add(TrackedBoss(mob))
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onArmorStandNameChange(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        val name = event.cleanName
        val steakBoss = trackedBosses.firstOrNull { it.armorStand === event.entity }
        if (isEnabled() && steakBoss != null) {
            processBossSteak(steakBoss, name)
        }
        val matches = name?.let { twinClawsPattern.matches(it) } == true
        val boss = if (matches) closestTrackedBoss(event.entity.getLorenzVec(), 5.0) else null
        if (!isEnabled() || !matches || boss == null) return
        if (!configBoss.twinClawsTitle) return
        if (twinClawsTitlePending) {
            return
        }
        if (nextTwinClawsTitle.passedSince() < 5.seconds) {
            return
        }
        val delay = config.twinclawsDelay.milliseconds
        twinClawsTitlePending = true
        runDelayed(delay) {
            twinClawsTitlePending = false
            if (!trackedBosses.contains(boss)) {
                return@runDelayed
            }
            if (nextTwinClawsTitle.passedSince() < 5.seconds) {
                return@runDelayed
            }
            nextTwinClawsTitle = SimpleTimeMark.now()
            TitleManager.sendTitle(
                "§6§lTWINCLAWS",
                duration = (1750 - config.twinclawsDelay).milliseconds,
            )
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!isEnabled()) return
        val entity = event.entity
        val newTexture = event.newItemStack?.getSkullTexture()
        val type = when (newTexture) {
            KILLER_SPRING_TEXTURE -> EffectType.KILLER_SPRING
            BLOOD_ICHOR_TEXTURE -> EffectType.BLOOD_ICHOR
            else -> null
        }
        val boss = type?.let { closestTrackedBoss(entity.getLorenzVec(), MAX_SLAYER_DISTANCE) }
        if (type != null && boss != null) {
            boss.effects[entity] = type
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        trackedBosses.clear()
    }

    @HandleEvent(onlyOnIsland = THE_RIFT, receiveCancelled = true)
    private fun onParticle(event: ParticleEvent) {
        if (event.type != ParticleTypes.ENCHANT) return
        if (!isEnabled()) return
        for (trackedBoss in trackedBosses) {
            val effects = trackedBoss.effects
            for ((stand, type) in effects.toMap()) {
                val standDistance = stand.distanceTo(event.location)
                if (standDistance > 3.0) continue
                val boss = closestTrackedBoss(event.location, MAX_SLAYER_DISTANCE)
                if (boss != null && boss !== trackedBoss) {
                    effects.remove(stand)
                    boss.effects[stand] = type
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (!configKillerSpring.fixSoundSpam) return

        if (event.soundName == "entity.wither.spawn") {
            if (lastWitherSpawnSound.passedSince() < 1.ticks) {
                ChatUtils.debug("Cancelling duplicate wither spawn sound sent within the same tick")
                return event.cancel()
            }
            lastWitherSpawnSound = ServerTimeMark.now()
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "slayer.vampireSlayerConfig", "slayer.vampire")
        event.move(148, "slayer.vampire.ownBoss", "slayer.vampire.boss")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (config.drawLine) {
            for (trackedBoss in trackedBosses) {
                event.renderBossLine(trackedBoss)
            }
        }
        if (!configBloodIchor.highlight && !configKillerSpring.highlight) {
            return
        }
        if (trackedBosses.isEmpty()) {
            return
        }
        for (trackedBoss in trackedBosses) {
            event.renderBossEffects(trackedBoss)
        }
    }

    private fun SkyHanniRenderWorldEvent.renderBossLine(trackedBoss: TrackedBoss) {
        val boss = trackedBoss.entity
        if (!boss.canBeSeen(MAX_SLAYER_DISTANCE)) return
        drawLineToCrosshair(
            exactLocation(boss).up(SNEAKING_EYE_HEIGHT),
            config.lineColor,
            config.lineWidth,
            true,
        )
    }

    private fun SkyHanniRenderWorldEvent.renderBossEffects(trackedBoss: TrackedBoss) {
        val boss = trackedBoss.entity as? RemotePlayer ?: return
        val effects = trackedBoss.effects
        val eyeY = exactPlayerEyeLocation().y
        val closestSprings = groupSpringStands(effects)
            .map { group -> group.minBy { abs(it.blockPosition().y + 0.5 - eyeY) } }
            .toSet()
        for ((stand, type) in effects) {
            if (type == KILLER_SPRING && stand !in closestSprings) continue
            renderEffect(boss, stand, type)
        }
    }

    private fun SkyHanniRenderWorldEvent.renderEffect(
        boss: RemotePlayer,
        stand: ArmorStand,
        type: EffectType,
    ) {
        val isIchor = type == BLOOD_ICHOR
        val isSpring = type == KILLER_SPRING
        if (!(isIchor && config.bloodIchor.highlight) && !(isSpring && config.killerSpring.highlight)) return
        val vec = stand.blockPosition().toLorenzVec()
        if (vec.distanceToPlayer() <= MAX_SLAYER_DISTANCE) {
            renderEffectHighlight(boss, stand, isIchor, isSpring, vec)
        }
        if (configBloodIchor.renderBeam && isIchor && stand.isAlive) {
            drawWaypointFilled(
                exactLocation(stand).add(0, y = -2, 0),
                configBloodIchor.color.toColor(),
                beacon = true,
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.renderEffectHighlight(
        boss: RemotePlayer,
        stand: ArmorStand,
        isIchor: Boolean,
        isSpring: Boolean,
        vec: LorenzVec,
    ) {
        val color = (if (isIchor) configBloodIchor.color else configKillerSpring.color)
            .toColor()
            .addAlpha(config.withAlpha)
        RenderLivingEntityHelper.setEntityColor(stand, color) {
            isEnabled() && trackedBosses.any { it.effects.containsKey(stand) }
        }
        val linesColor = (if (isIchor) configBloodIchor.linesColor else configKillerSpring.linesColor).toColor()
        drawColor(vec.up(2.0), LorenzColor.DARK_RED.toChromaColor(), alpha = 1f)
        drawDynamicText(
            vec.add(0.5, 2.5, 0.5),
            if (isIchor) "§4Ichor" else "§4Spring",
            1.5,
            seeThroughBlocks = false,
        )
        val showLines = (configBloodIchor.showLines && isIchor) || (configKillerSpring.showLines && isSpring)
        if (showLines && stand.canBeSeen(vecYOffset = 1.5)) {
            draw3DLine(
                exactPlayerEyeLocation(boss),
                exactPlayerEyeLocation(stand),
                linesColor,
                3,
                true,
            )
        }
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau()
}
