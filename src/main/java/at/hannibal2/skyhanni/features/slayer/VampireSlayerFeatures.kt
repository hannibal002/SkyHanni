package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
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
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.PlayerUtils.SNEAKING_EYE_HEIGHT
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.EntityCompat.findHealthReal
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


    private data class ArmorStandEffect(val stand: ArmorStand, val type: EffectType) {
        enum class EffectType {
            BLOOD_ICHOR,
            KILLER_SPRING
        }
    }

    private val bosses = linkedSetOf<RemotePlayer>()
    private val effectStands = linkedSetOf<ArmorStandEffect>()
    private val standList = mutableMapOf<ArmorStand, RemotePlayer>()
    private val steakReadyBosses = mutableSetOf<Int>()

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

    private fun RemotePlayer.isHighlighted(): Boolean = bosses.contains(this)

    private val logger = SkyHanniLogger("slayer/vampire")
    private fun log(message: String) = logger.log(message)

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        if (!configBoss.highlight) return
        val removed = bosses.filter { it.distanceToPlayer() > 15 }
        bosses.removeAll(removed.toSet())
        log("player move cleanup: removed=${removed.map { it.id }}, remaining=${bosses.size}")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        val entity = event.entity as? RemotePlayer ?: run {
            log("health update ignored: entity is ${event.entity.javaClass.simpleName}")
            return
        }
        log(
            "health update: id=${entity.id}, cleanName='${entity.cleanName}', rawName='${entity.name}', " +
                "position=${entity.blockPosition()}, health=${event.health}, " +
                "tracked=${entity in bosses}",
        )
        if (!isEnabled()) return
        if (!bossNamePattern.matches(entity.cleanName)) {
            log(
                "health update rejected: id=${entity.id}, cleanName='${entity.cleanName}', " +
                    "rawName='${entity.name}', pattern='Bloodfiend .*'",
            )
            bosses.remove(entity)
            return
        }
        bosses.add(entity)
        processBossHealth(entity, "health update")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityEnterWorld(event: EntityEnterWorldEvent<RemotePlayer>) {
        log(
            "remote player entered: id=${event.entity.id}, cleanName='${event.entity.cleanName}', " +
                "rawName='${event.entity.name}', position=${event.entity.blockPosition()}",
        )
        if (!isEnabled()) return
        if (bossNamePattern.matches(event.entity.cleanName)) {
            bosses.add(event.entity)
            log("boss added on enter: id=${event.entity.id}, tracked=${bosses.size}")
        } else {
            log(
                "entity enter rejected: id=${event.entity.id}, cleanName='${event.entity.cleanName}', " +
                    "rawName='${event.entity.name}', pattern='Bloodfiend .*'",
            )
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<RemotePlayer>) {
        log(
            "remote player name update: id=${event.entity.id}, cleanName='${event.entity.cleanName}', " +
                "rawName='${event.newName}', currentRawName='${event.entity.name}'",
        )
        if (!isEnabled()) return
        if (bossNamePattern.matches(event.entity.cleanName)) {
            bosses.add(event.entity)
            log("boss added on name update: id=${event.entity.id}, tracked=${bosses.size}")
            processBossHealth(event.entity, "name update")
        } else {
            bosses.remove(event.entity)
            log(
                "boss removed on name update: id=${event.entity.id}, cleanName='${event.entity.cleanName}', " +
                    "rawName='${event.newName}', pattern='Bloodfiend .*', tracked=${bosses.size}",
            )
        }
    }

    private fun processBossHealth(entity: RemotePlayer, source: String) {
        val realHealth = entity.findHealthReal()
        val maxHealth = entity.baseMaxHealth
        val canUseSteak = maxHealth > 0 && realHealth <= maxHealth * 0.2f
        log(
            "boss health processed: source=$source, id=${entity.id}, realHealth=$realHealth, " +
                "maxHealth=$maxHealth, canUseSteak=$canUseSteak, tracked=${entity in bosses}",
        )
        if (canUseSteak) {
            if (steakReadyBosses.add(entity.id) && configBoss.steakAlert) {
                log("sending steak title: bossId=${entity.id}, source=$source")
                TitleManager.sendTitle("§c§lSTEAK!", duration = 300.milliseconds)
            }
        } else {
            steakReadyBosses.remove(entity.id)
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
    private fun onArmorStandNameChange(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        val name = event.cleanName
        val matches = name?.let { twinClawsPattern.matches(it) } == true
        log(
            "armor stand name update: id=${event.entity.id}, cleanName='$name', " +
                "newNameString='${event.newName?.string}', newNameComponent='${event.newName}', " +
                "matchesTwinclaws=$matches, pattern='.*TWINCLAWS.*', " +
                "position=${event.entity.blockPosition()}",
        )
        if (!isEnabled() || !matches) return
        if (!configBoss.twinClawsTitle) return
        if (nextTwinClawsTitle.passedSince() < 5.seconds) {
            log("twinclaws title throttled: id=${event.entity.id}, name='$name'")
            return
        }
        val delay = config.twinclawsDelay.milliseconds
        log("twinclaws detected: id=${event.entity.id}, name='$name', delay=$delay")
        runDelayed(delay) {
            if (nextTwinClawsTitle.passedSince() < 5.seconds) {
                log("delayed twinclaws title throttled: id=${event.entity.id}, name='$name'")
                return@runDelayed
            }
            nextTwinClawsTitle = SimpleTimeMark.now()
            log("sending twinclaws title: standId=${event.entity.id}, name='$name'")
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
        effectStands.removeIf { it.stand == entity }
        val newTexture = event.newItemStack?.getSkullTexture()
        when (newTexture) {
            KILLER_SPRING_TEXTURE -> {
                effectStands.add(ArmorStandEffect(entity, KILLER_SPRING))
                log("killer spring detected: id=${entity.id}, effects=${effectStands.size}")
            }

            BLOOD_ICHOR_TEXTURE -> {
                effectStands.add(ArmorStandEffect(entity, BLOOD_ICHOR))
                log("blood ichor detected: id=${entity.id}, effects=${effectStands.size}")
            }

            else -> {
                log("effect stand removed: id=${entity.id}, effects=${effectStands.size}")
            }
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<*>) {
        log("entity leaving: id=${event.entity.id}, type=${event.entity.javaClass.simpleName}")
        when (val entity = event.entity) {
            is RemotePlayer -> {
                bosses.remove(entity)
                steakReadyBosses.remove(entity.id)
            }

            is ArmorStand -> effectStands.removeIf { entity == it.stand }
        }
        standList.entries.removeIf { it.key === event.entity || it.value === event.entity }
        log("leave cleanup: bosses=${bosses.size}, effects=${effectStands.size}, associations=${standList.size}")
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        log("render: bosses=${bosses.size}, effects=${effectStands.size}, associations=${standList.size}")

        if (config.drawLine) {
            for (it in bosses) {
                val visible = it.canBeSeen(15)
                log("boss line candidate: id=${it.id}, visible=$visible")
                if (!it.isHighlighted() || !visible) continue
                val vec = event.exactLocation(it)
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
        if (bosses.isEmpty()) {
            log("effect rendering skipped: no tracked bosses, effects=${effectStands.size}")
            return
        }
        for ((stand, type) in effectStands) {
            val vec = stand.blockPosition().toLorenzVec()
            val distance = vec.distanceToPlayer()
            val isIchor = type == BLOOD_ICHOR
            val isSpring = type == KILLER_SPRING
            log("effect candidate: standId=${stand.id}, type=$type, distance=$distance")
            if (!(isIchor && config.bloodIchor.highlight) && !(isSpring && config.killerSpring.highlight)) continue
            val color = (if (isIchor) configBloodIchor.color else configKillerSpring.color).toColor().addAlpha(config.withAlpha)
            if (distance <= 15) {
                RenderLivingEntityHelper.setEntityColor(
                    stand,
                    color,
                ) { isEnabled() }

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
                    val boss = standList[stand]
                    if (boss != null && stand.canBeSeen(vecYOffset = 1.5)) {
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

    @HandleEvent
    private fun onWorldChange() {
        log("world change: bosses=${bosses.size}, effects=${effectStands.size}, associations=${standList.size}")
        bosses.clear()
        effectStands.clear()
        standList.clear()
        steakReadyBosses.clear()
    }

    @HandleEvent(onlyOnIsland = THE_RIFT, receiveCancelled = true)
    private fun onParticle(event: ParticleEvent) {
        log("particle: type=${event.type}, location=${event.location}")
        if (event.type != ParticleTypes.ENCHANT) return
        if (!isEnabled()) return
        for ((stand, _) in effectStands) {
            val standDistance = stand.distanceTo(event.location)
            if (standDistance > 3.0) continue
            val boss = bosses
                .filter { it.isHighlighted() }
                .minByOrNull { it.distanceTo(event.location) }
            log(
                "particle candidate: standId=${stand.id}, standDistance=$standDistance, " +
                    "nearestBossId=${boss?.id}",
            )
            if (boss != null) {
                standList[stand] = boss
                log("effect associated: standId=${stand.id}, bossId=${boss.id}")
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
