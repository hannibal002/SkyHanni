package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsInRadiusWith
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

// TODO: optimize this entire class, its so bad
@OptIn(AllEntitiesGetter::class)
@SkyHanniModule
object VampireSlayerFeatures {

    private val config get() = SlayerApi.config.vampire
    private val configOwnBoss get() = config.ownBoss
    private val configOtherBoss get() = config.othersBoss
    private val configCoopBoss get() = config.coopBoss
    private val configBloodIchor get() = config.bloodIchor
    private val configKillerSpring get() = config.killerSpring

    private val entityList = mutableListOf<LivingEntity>()
    private val taggedEntityList = mutableListOf<Int>()
    private var standList = mapOf<ArmorStand, RemotePlayer>()

    // Nicked support
    private val username
        get() = EntityUtils.getEntities<LocalPlayer>().firstOrNull()?.name.formattedTextCompatLessResets() ?: error("own player is null")

    private val BLOOD_ICHOR_TEXTURE by lazy { SkullTextureHolder.getTexture("BLOOD_ICHOR") }
    private val KILLER_SPRING_TEXTURE by lazy { SkullTextureHolder.getTexture("KILLER_SPRING") }

    private var nextClawSend = 0L
    private var lastWitherSpawnSound = ServerTimeMark.farPast()

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        val start = LocationUtils.playerLocation()
        if (configOwnBoss.highlight || configOtherBoss.highlight || configCoopBoss.highlight) {
            for (player in EntityUtils.getEntities<RemotePlayer>()) {
                val distance = start.distance(player.blockPosition().toLorenzVec())
                if (distance <= 15)
                    player.process()
            }
        }
        if (configBloodIchor.highlight || configKillerSpring.highlight) {
            for (stand in EntityUtils.getEntities<ArmorStand>()) {
                val vec = stand.blockPosition().toLorenzVec()
                val distance = start.distance(vec)
                val isIchor = stand.hasSkullTexture(BLOOD_ICHOR_TEXTURE)
                if (!isIchor && !stand.hasSkullTexture(KILLER_SPRING_TEXTURE)) continue
                val chromaColour = if (isIchor) configBloodIchor.color else configKillerSpring.color
                val color = chromaColour.toColor().addAlpha(config.withAlpha)
                if (distance > 15) continue
                RenderLivingEntityHelper.setEntityColor(stand, color) { isEnabled() }
                if (isIchor) {
                    entityList.add(stand)
                }
            }
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return
        entityList.editCopy { removeIf { it.deceased } }
    }

    private fun List<String>.spawnedByCoop(stand: ArmorStand): Boolean = any {
        var contain = false
        if (".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*).*".toRegex().matches(stand.name.formattedTextCompatLessResets())) {
            val name = ".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*)".toRegex()
                .find(stand.name.formattedTextCompatLessResets())?.groupValues?.get(1)
            contain = it == name
        }
        contain
    }

    private fun RemotePlayer.process() {
        if (name.formattedTextCompatLessResets() != "Bloodfiend ") return

        if (configOwnBoss.twinClawsTitle || configOtherBoss.twinClawsTitle || configCoopBoss.twinClawsTitle) {
            for (stand in getAllNameTagsInRadiusWith("TWINCLAWS")) {
                if (!".*(?:§(?:\\d|\\w))+TWINCLAWS (?:§(?:\\w|\\d))+[0-9.,]+s.*".toRegex()
                        .matches(stand.name.formattedTextCompatLessResets())
                ) continue
                val coopList = configCoopBoss.coopMembers.split(",").toList()
                val containUser = getAllNameTagsInRadiusWith("Spawned by").any {
                    it.name.formattedTextCompatLessResets().contains(username)
                }
                val containCoop = getAllNameTagsInRadiusWith("Spawned by").any {
                    configCoopBoss.highlight && coopList.spawnedByCoop(it)
                }
                val shouldSendTitle =
                    if (containUser && configOwnBoss.twinClawsTitle) true
                    else if (containCoop && configCoopBoss.twinClawsTitle) true
                    else taggedEntityList.contains(this.id) && configOtherBoss.twinClawsTitle

                if (!shouldSendTitle) continue
                DelayedRun.runDelayed(config.twinclawsDelay.milliseconds) {
                    if (nextClawSend < System.currentTimeMillis()) {
                        TitleManager.sendTitle(
                            "§6§lTWINCLAWS",
                            duration = (1750 - config.twinclawsDelay).milliseconds,
                        )
                        nextClawSend = System.currentTimeMillis() + 5_000
                    }
                }
            }
        }
        for (it in getAllNameTagsInRadiusWith("Spawned by")) {
            val coopList = configCoopBoss.coopMembers.split(",").toList()
            val containUser = it.name.formattedTextCompatLessResets().contains(username)
            val containCoop = coopList.spawnedByCoop(it)
            val neededHealth = baseMaxHealth * 0.2f
            if (containUser && taggedEntityList.contains(id)) {
                taggedEntityList.remove(id)
            }
            val canUseSteak = findHealthReal() <= neededHealth
            val ownBoss = configOwnBoss.highlight && containUser && isNpc()
            val otherBoss = configOtherBoss.highlight && taggedEntityList.contains(id) && isNpc()
            val coopBoss = configCoopBoss.highlight && containCoop && isNpc()
            val shouldRender = if (ownBoss) true else if (otherBoss) true else coopBoss

            val color = when {
                canUseSteak && config.changeColorWhenCanSteak -> config.steakColor.toColor()
                ownBoss -> configOwnBoss.highlightColor.toColor()
                otherBoss -> configOtherBoss.highlightColor.toColor()
                coopBoss -> configCoopBoss.highlightColor.toColor()
                else -> Color.BLACK
            }

            val shouldSendSteakTitle =
                if (canUseSteak && configOwnBoss.steakAlert && containUser) true
                else if (canUseSteak && configOtherBoss.steakAlert && taggedEntityList.contains(id)) true
                else canUseSteak && configCoopBoss.steakAlert && containCoop

            if (shouldSendSteakTitle) {
                TitleManager.sendTitle("§c§lSTEAK!", duration = 300.milliseconds)
            }

            if (shouldRender) {
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(this, color) { isEnabled() }
                entityList.add(this)
            }
        }
    }

    private fun RemotePlayer.isHighlighted(): Boolean {
        return entityList.contains(this) || taggedEntityList.contains(id)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.clickedEntity !is RemotePlayer) return
        if (!event.clickedEntity.isNpc()) return
        val coopList = configCoopBoss.coopMembers.split(",").toList()
        val regexA = ".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*).*".toRegex()
        val regexB = ".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*)".toRegex()
        for (armorStand in event.clickedEntity.getAllNameTagsInRadiusWith("Spawned by")) {
            val containCoop = coopList.isNotEmpty() &&
                coopList.any {
                    var contain = false
                    if (regexA.matches(armorStand.name.formattedTextCompatLessResets())) {
                        val name = regexB.find(armorStand.name.formattedTextCompatLessResets())?.groupValues?.get(1)
                        contain = it == name
                    }
                    contain
                }
            if (armorStand.name.formattedTextCompatLessResets().contains(username) || containCoop) return
            if (!taggedEntityList.contains(event.clickedEntity.id)) {
                taggedEntityList.add(event.clickedEntity.id)
            }
        }
    }

    @HandleEvent
    fun onEntityDeath(event: EntityDeathEvent<*>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entityList.contains(entity)) {
            entityList.remove(entity)
        }
        if (taggedEntityList.contains(entity.id)) {
            taggedEntityList.remove(entity.id)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (config.drawLine) {
            for (it in EntityUtils.getEntities<RemotePlayer>()) {
                if (!it.isHighlighted()) continue
                if (!it.canBeSeen(15)) continue
                val vec = event.exactLocation(it)
                event.drawLineToEye(
                    vec.up(1.54),
                    config.lineColor,
                    config.lineWidth,
                    true,
                )
            }
        }
        if (!configBloodIchor.highlight && !configKillerSpring.highlight) return
        for (stand in EntityUtils.getAllEntities().filterIsInstance<ArmorStand>()) {
            val vec = stand.blockPosition().toLorenzVec()
            val distance = vec.distanceToPlayer()
            val isIchor = stand.hasSkullTexture(BLOOD_ICHOR_TEXTURE)
            val isSpring = stand.hasSkullTexture(KILLER_SPRING_TEXTURE)
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
    fun onWorldChange() {
        entityList.clear()
        taggedEntityList.clear()
        standList = mutableMapOf()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val loc = event.location
        for (boss in EntityUtils.getEntitiesNearby<RemotePlayer>(loc, 3.0)) {
            if (!boss.isHighlighted() || event.type != ParticleTypes.ENCHANT) continue
            for (ichor in EntityUtils.getEntitiesNearby<ArmorStand>(event.location, 3.0)) {
                if (ichor.hasSkullTexture(KILLER_SPRING_TEXTURE) || ichor.hasSkullTexture(BLOOD_ICHOR_TEXTURE)) {
                    standList = standList.editCopy { this[ichor] = boss }
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (!configKillerSpring.fixSoundSpam) return

        if (event.soundName == "mob.wither.spawn") {
            if (lastWitherSpawnSound.passedSince() < 1.ticks) {
                ChatUtils.debug("Cancelling duplicate wither spawn sound sent within the same tick")
                return event.cancel()
            }
            lastWitherSpawnSound = ServerTimeMark.now()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "slayer.vampireSlayerConfig", "slayer.vampire")
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau()
}
