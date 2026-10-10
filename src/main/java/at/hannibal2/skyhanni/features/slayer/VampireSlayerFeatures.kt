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
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.PlayerUtils.SNEAKING_EYE_HEIGHT
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
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

@SkyHanniModule
object VampireSlayerFeatures {

    private val config get() = SlayerApi.config.vampire
    private val configBoss get() = config.boss
    private val configBloodIchor get() = config.bloodIchor
    private val configKillerSpring get() = config.killerSpring


    private data class ArmorStandEffect(val stand: ArmorStand, val type: EffectType) {
        enum class EffectType {
            BLOOD_ICHOR, KILLER_SPRING
        }
    }

    private val bosses = linkedSetOf<RemotePlayer>()
    private val effectStands = linkedSetOf<ArmorStandEffect>()
    private val standList = mutableMapOf<ArmorStand, RemotePlayer>()

    private val BLOOD_ICHOR_TEXTURE by SkullTextureHolder.texture("BLOOD_ICHOR")
    private val KILLER_SPRING_TEXTURE by SkullTextureHolder.texture("KILLER_SPRING")

    private var lastWitherSpawnSound = ServerTimeMark.farPast()

    private val patternGroup = RepoPattern.group("slayer.vampire-features")

    /**
     * WRAPPED-REGEX-TEST: "Bloodfiend "
     */
    private val bossNamePattern by patternGroup.pattern(
        "boss-name",
        "Bloodfiend .*",
    )

    private fun RemotePlayer.isHighlighted(): Boolean = bosses.contains(this)

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        if (!configBoss.highlight) return
        bosses.removeIf { it.distanceToPlayer() > 15 }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return
        if (!configBoss.highlight) return
        val entity = event.entity as? RemotePlayer ?: return
        if (!bossNamePattern.matches(entity.cleanName)) {
            bosses.remove(entity)
            return
        }
        bosses.add(entity)
        val canUseSteak = entity.findHealthReal() <= entity.baseMaxHealth * 0.2f
        val color = if (canUseSteak && config.changeColorWhenCanSteak) {
            config.steakColor.toColor()
        } else {
            configBoss.highlightColor.toColor()
        }
        RenderLivingEntityHelper.setEntityColor(entity, color) { isEnabled() }
        if (canUseSteak && configBoss.steakAlert) {
            TitleManager.sendTitle("§c§lSTEAK!", duration = 300.milliseconds)
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityEnterWorld(event: EntityEnterWorldEvent<RemotePlayer>) {
        if (!isEnabled()) return
        if (bossNamePattern.matches(event.entity.cleanName)) bosses.add(event.entity)
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityNameUpdate(event: EntityCustomNameUpdateEvent<RemotePlayer>) {
        if (!isEnabled()) return
        if (bossNamePattern.matches(event.entity.cleanName)) {
            bosses.add(event.entity)
        } else {
            bosses.remove(event.entity)
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!isEnabled()) return
        val entity = event.entity
        when {
            entity.wearingSkullTexture(KILLER_SPRING_TEXTURE) -> {
                effectStands.add(ArmorStandEffect(entity, KILLER_SPRING))
            }
            entity.wearingSkullTexture(BLOOD_ICHOR_TEXTURE) -> {
                effectStands.add(ArmorStandEffect(entity, BLOOD_ICHOR))
            }
            else -> {
                effectStands.removeIf { it.stand == entity }
            }
        }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<*>) {
        when (val entity = event.entity) {
            is RemotePlayer -> bosses.remove(entity)
            is ArmorStand -> effectStands.removeIf { entity == it.stand }
        }
        standList.entries.removeIf { it.key === event.entity || it.value === event.entity }
    }

    @HandleEvent(onlyOnIsland = THE_RIFT)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (config.drawLine) {
            for (it in bosses) {
                if (!it.isHighlighted()) continue
                if (!it.canBeSeen(15)) continue
                val vec = event.exactLocation(it)
                event.drawLineToCrosshair(
                    vec.up(SNEAKING_EYE_HEIGHT),
                    config.lineColor,
                    config.lineWidth,
                    true,
                )
            }
        }
        if (!configBloodIchor.highlight && !configKillerSpring.highlight) return
        for ((stand, type) in effectStands) {
            val vec = stand.blockPosition().toLorenzVec()
            val distance = vec.distanceToPlayer()
            val isIchor = type == BLOOD_ICHOR
            val isSpring = type == KILLER_SPRING
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
                for ((ichor, boss) in standList) {
                    if (!(configBloodIchor.showLines && isIchor) && !(configKillerSpring.showLines && isSpring)) continue

                    // ichors are sometimes in the ground
                    if (!ichor.canBeSeen(vecYOffset = 1.5)) continue
                    event.draw3DLine(
                        event.exactPlayerEyeLocation(boss),
                        event.exactPlayerEyeLocation(ichor),
                        linesColorStart,
                        3,
                        true,
                    )

                }
            }
            if (configBloodIchor.renderBeam && isIchor && stand.isAlive) {
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
        bosses.clear()
        effectStands.clear()
        standList.clear()
    }

    @HandleEvent(onlyOnIsland = THE_RIFT, receiveCancelled = true)
    private fun onParticle(event: ParticleEvent) {
        if (event.type != ParticleTypes.ENCHANT) return
        if (!isEnabled()) return
        for (boss in bosses) {
            if (!boss.isHighlighted()) continue
            for ((stand, _) in effectStands) {
                if (stand.distanceTo(event.location) <= 3.0) {
                    standList[stand] = boss
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

    fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau()
}
