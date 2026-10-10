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
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils.SNEAKING_EYE_HEIGHT
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
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
        KILLER_SPRING
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

    private fun trackedBoss(entity: RemotePlayer): TrackedBoss? =
        trackedBosses.firstOrNull { it.entity === entity }

    private fun closestTrackedBoss(position: LorenzVec, range: Double): TrackedBoss? =
        trackedBosses
            .map { it to it.entity.distanceTo(position) }
            .filter { it.second <= range }
            .minByOrNull { it.second }
            ?.first

    private val logger = SkyHanniLogger("slayer/vampire")
    private fun log(message: String) = logger.log(message)

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        val removed = trackedBosses.filter { it.entity.distanceToPlayer() > 15 }
        trackedBosses.removeAll(removed)
        log("player move cleanup: removed=${removed.map { it.entity.id }}, remaining=${trackedBosses.size}")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.baseEntity !is RemotePlayer || !bossNamePattern.matches(mob.baseEntity.cleanName)) return
        log("vampire mob spawned: id=${mob.baseEntity.id}, mobName='${mob.name}', armorStandId=${mob.armorStand?.id}")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val removed = trackedBosses.removeIf { it.mob === event.mob }
        if (removed) {
            log("tracked vampire mob despawned: id=${event.mob.baseEntity.id}, remaining=${trackedBosses.size}")
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEffectLeaveWorld(event: EntityLeaveWorldEvent<ArmorStand>) {
        trackedBosses.forEach { it.effects.remove(event.entity) }
    }

    private fun processBossSteak(boss: TrackedBoss, healthName: String?) {
        val entity = boss.entity as? RemotePlayer ?: return
        val canUseSteak = healthName?.let { steakHealthPattern.matches(it) } == true
        log(
            "boss steak processed: id=${entity.id}, mob=${boss.mob.name}, " +
                "healthStandId=${boss.armorStand?.id}, healthName='$healthName', " +
                "canUseSteak=$canUseSteak, tracked=true",
        )
        if (canUseSteak) {
            if (!boss.steakAlertSent && configBoss.steakAlert) {
                boss.steakAlertSent = true
                log("sending steak title: bossId=${entity.id}")
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
        RenderLivingEntityHelper.setEntityColor(entity, color) { isEnabled() }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled() || event.action != EntityClickEvent.ActionType.ATTACK) return
        val entity = event.clickedEntity as? RemotePlayer ?: return
        val cleanName = entity.cleanName
        val matches = bossNamePattern.matches(cleanName)
        log(
            "entity attacked: id=${entity.id}, cleanName='$cleanName', rawName='${entity.name}', " +
                "matchesBoss=$matches, alreadyTracked=${trackedBoss(entity) != null}",
        )
        if (!matches) return
        val mob = entity.mob
        if (mob == null || mob.baseEntity !== entity) return
        if (trackedBoss(entity) == null) {
            trackedBosses.add(TrackedBoss(mob))
        }
        log("boss tracked after attack: id=${entity.id}, tracked=${trackedBosses.size}")
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
        log(
            "armor stand name update: id=${event.entity.id}, cleanName='$name', " +
                "newNameString='${event.newName?.string}', newNameComponent='${event.newName}', " +
                "matchesTwinclaws=$matches, pattern='.*TWINCLAWS.*', " +
                "nearbyTrackedBossId=${boss?.entity?.id}, nearbyTrackedBossDistance=${boss?.entity?.distanceTo(event.entity.getLorenzVec())}, " +
                "requiredRange=5.0, position=${event.entity.blockPosition()}",
        )
        if (!isEnabled() || !matches || boss == null) return
        if (!configBoss.twinClawsTitle) return
        if (nextTwinClawsTitle.passedSince() < 5.seconds) {
            log("twinclaws title throttled: id=${event.entity.id}, bossId=${boss.entity.id}, name='$name'")
            return
        }
        val delay = config.twinclawsDelay.milliseconds
        log("twinclaws detected: id=${event.entity.id}, bossId=${boss.entity.id}, name='$name', delay=$delay")
        runDelayed(delay) {
            if (!trackedBosses.contains(boss)) {
                log("delayed twinclaws ignored: boss is no longer tracked, bossId=${boss.entity.id}")
                return@runDelayed
            }
            if (nextTwinClawsTitle.passedSince() < 5.seconds) {
                log("delayed twinclaws title throttled: id=${event.entity.id}, bossId=${boss.entity.id}, name='$name'")
                return@runDelayed
            }
            nextTwinClawsTitle = SimpleTimeMark.now()
            log("sending twinclaws title: standId=${event.entity.id}, bossId=${boss.entity.id}, name='$name'")
            TitleManager.sendTitle(
                "§6§lTWINCLAWS",
                duration = (1750 - config.twinclawsDelay).milliseconds,
            )
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        log(
            "armor stand equipment update: id=${event.entity.id}, position=${event.entity.blockPosition()}, " +
                "slot=${event.equipmentSlot}, newTexture=${event.newItemStack?.getSkullTexture()}, " +
                "killerSpring=${event.entity.hasSkullTexture(KILLER_SPRING_TEXTURE)}, " +
                "bloodIchor=${event.entity.hasSkullTexture(BLOOD_ICHOR_TEXTURE)}",
        )
        if (!isEnabled()) return
        val entity = event.entity
        trackedBosses.forEach { it.effects.remove(entity) }
        val newTexture = event.newItemStack?.getSkullTexture()
        val type = when (newTexture) {
            KILLER_SPRING_TEXTURE -> EffectType.KILLER_SPRING
            BLOOD_ICHOR_TEXTURE -> EffectType.BLOOD_ICHOR
            else -> null
        }
        val boss = type?.let { closestTrackedBoss(entity.getLorenzVec(), 15.0) }
        if (type != null && boss != null) {
            boss.effects[entity] = type
            log("effect detected: id=${entity.id}, type=$type, bossId=${boss.entity.id}")
        } else {
            log("effect stand removed or unowned: id=${entity.id}, type=$type")
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        log("render: bosses=${trackedBosses.size}, effects=${trackedBosses.sumOf { it.effects.size }}")

        if (config.drawLine) {
            for (trackedBoss in trackedBosses) {
                val boss = trackedBoss.entity
                val visible = boss.canBeSeen(15)
                log("boss line candidate: id=${boss.id}, visible=$visible")
                if (!visible) continue
                val vec = event.exactLocation(boss)
                event.drawLineToCrosshair(
                    vec.up(SNEAKING_EYE_HEIGHT),
                    config.lineColor,
                    config.lineWidth,
                    true,
                )
            }
        }
        if (!configBloodIchor.highlight && !configKillerSpring.highlight) {
            return
        }
        if (trackedBosses.isEmpty()) {
            log("effect rendering skipped: no tracked bosses, effects=0")
            return
        }
        for (trackedBoss in trackedBosses) {
            val boss = trackedBoss.entity
            val effects = trackedBoss.effects
            for ((stand, type) in effects) {
                val vec = stand.blockPosition().toLorenzVec()
                val distance = vec.distanceToPlayer()
                val isIchor = type == BLOOD_ICHOR
                val isSpring = type == KILLER_SPRING
                log("effect candidate: standId=${stand.id}, type=$type, distance=$distance, bossId=${boss.id}")
                if (!(isIchor && config.bloodIchor.highlight) && !(isSpring && config.killerSpring.highlight)) {
                    continue
                }
                val color = (if (isIchor) configBloodIchor.color else configKillerSpring.color)
                    .toColor()
                    .addAlpha(config.withAlpha)
                if (distance <= 15) {
                    RenderLivingEntityHelper.setEntityColor(stand, color) { isEnabled() }

                    val linesColorStart =
                        (if (isIchor) configBloodIchor.linesColor else configKillerSpring.linesColor).toColor()
                    val text = if (isIchor) "§4Ichor" else "§4Spring"
                    event.drawColor(
                        stand.blockPosition().toLorenzVec().up(2.0),
                        LorenzColor.DARK_RED.toChromaColor(),
                        alpha = 1f,
                    )
                    event.drawDynamicText(
                        stand.blockPosition().toLorenzVec().add(0.5, 2.5, 0.5),
                        text,
                        1.5,
                        seeThroughBlocks = false,
                    )
                    if ((configBloodIchor.showLines && isIchor) || (configKillerSpring.showLines && isSpring)) {
                        if (stand.canBeSeen(vecYOffset = 1.5)) {
                            log("rendering effect line: standId=${stand.id}, bossId=${boss.id}, type=$type")
                            event.draw3DLine(
                                event.exactPlayerEyeLocation(boss),
                                event.exactPlayerEyeLocation(stand),
                                linesColorStart,
                                3,
                                true,
                            )
                        }
                    }
                }
                if (configBloodIchor.renderBeam && isIchor && stand.isAlive) {
                    log("rendering blood ichor beam: standId=${stand.id}")
                    event.drawWaypointFilled(
                        event.exactLocation(stand).add(0, y = -2, 0),
                        configBloodIchor.color.toColor(),
                        beacon = true,
                    )
                }
            }
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        log("world change: bosses=${trackedBosses.size}, effects=${trackedBosses.sumOf { it.effects.size }}")
        trackedBosses.clear()
    }

    @HandleEvent(onlyOnIsland = THE_RIFT, receiveCancelled = true)
    private fun onParticle(event: ParticleEvent) {
        log("particle: type=${event.type}, location=${event.location}")
        if (event.type != ParticleTypes.ENCHANT) return
        if (!isEnabled()) return
        for (trackedBoss in trackedBosses) {
            val effects = trackedBoss.effects
            for ((stand, type) in effects.toMap()) {
                val standDistance = stand.distanceTo(event.location)
                if (standDistance > 3.0) continue
                val boss = closestTrackedBoss(event.location, 15.0)
                log(
                    "particle candidate: standId=${stand.id}, standDistance=$standDistance, " +
                        "nearestBossId=${boss?.entity?.id}",
                )
                if (boss != null && boss !== trackedBoss) {
                    effects.remove(stand)
                    boss.effects[stand] = type
                    log("effect associated: standId=${stand.id}, bossId=${boss.entity.id}")
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
                log("duplicate wither sound cancelled")
                ChatUtils.debug("Cancelling duplicate wither spawn sound sent within the same tick")
                return event.cancel()
            }
            lastWitherSpawnSound = ServerTimeMark.now()
            log("wither sound accepted")
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "slayer.vampireSlayerConfig", "slayer.vampire")
        event.move(148, "slayer.vampire.ownBoss", "slayer.vampire.boss")
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau()
}
