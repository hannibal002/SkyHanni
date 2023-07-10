package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsInRadiusWith
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class VampireSlayerFeatures {

    private val config get() = SkyHanniMod.feature.rift.vampireSlayerFeatures
    private val entityList = mutableListOf<EntityLivingBase>()
    private val taggedEntityList = mutableListOf<Int>()
    private val username = Minecraft.getMinecraft().session.username
    private val bloodIchorTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzAzNDA5MjNhNmRlNDgyNWExNzY4MTNkMTMzNTAzZWZmMTg2ZGIwODk2ZTMyYjY3MDQ5MjhjMmEyYmY2ODQyMiJ9fX0="
    private val killerSpringTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzdmN2E3YmM4YWM4NmYyM2NhN2JmOThhZmViNzY5NjAyMjdlMTgzMmZlMjA5YTMwMjZmNmNlYjhiZGU3NGY1NCJ9fX0="

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        val start = LocationUtils.playerLocation()
        if (config.highlightOwnBoss || config.highlightOthers) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach {
                val vec = it.position.toLorenzVec()
                val distance = start.distance(vec)
                if (distance <= 20)
                    it.process()
            }
        }
        if (config.bloodIchor.highlight || config.killerSpring.highlight) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>().forEach { stand ->
                val vec = stand.position.toLorenzVec()
                val distance = start.distance(vec)
                if (stand.hasSkullTexture(bloodIchorTexture) || stand.hasSkullTexture(killerSpringTexture)) {
                    val isIchor = stand.hasSkullTexture(bloodIchorTexture)
                    val color = if (isIchor) config.bloodIchor.color.toChromaColor().withAlpha(config.withAlpha)
                    else if (stand.hasSkullTexture(killerSpringTexture)) config.killerSpring.color.toChromaColor().withAlpha(config.withAlpha)
                    else LorenzColor.WHITE.toColor().withAlpha(config.withAlpha)
                    RenderLivingEntityHelper.setEntityColor(
                        stand,
                        color
                    ) { distance <= 20 }
                    if (isIchor && distance <= 20)
                        entityList.add(stand)
                }
            }
        }
    }

    private fun EntityOtherPlayerMP.process() {
        if (name != "Bloodfiend ") return
        if (config.twinClawsTitle) {
            getAllNameTagsInRadiusWith("TWINCLAWS").forEach { stand ->
                if (".*(?:§(?:\\d|\\w))+TWINCLAWS (?:§(?:\\w|\\d))+[0-9.,]+s.*".toRegex().matches(stand.name)) {
                    val coopList = config.coopsBossHighlight.coopMembers.split(",").toList()
                    val containUser = getAllNameTagsInRadiusWith("Spawned by").any {
                        it.name.contains(username)
                    }
                    val containCoop = getAllNameTagsInRadiusWith("Spawned by").any {
                        coopList.isNotEmpty() && config.coopsBossHighlight.highlight && coopList.any { it2 ->
                            var contain = false
                            if (".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*).*".toRegex().matches(it.name)) {
                                val name = ".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*)".toRegex().find(it.name)?.groupValues?.get(1)
                                contain = it2 == name
                            }
                            contain
                        }
                    }
                    if (containUser || containCoop || taggedEntityList.contains(this.entityId)) {
                        val color = if (containUser) "§6" else if (taggedEntityList.contains(this.entityId)) "§c" else if (containCoop) "§3" else "§f"
                        TitleUtils.sendTitle("$color§lTWINCLAWS", 300, 2.6)
                    }
                }
            }
        }
        getAllNameTagsInRadiusWith("Spawned by").forEach {
            val coopList = config.coopsBossHighlight.coopMembers.split(",").toList()
            val containUser = it.name.contains(username)
            val containCoop = coopList.isNotEmpty() && coopList.any { it2 ->
                var contain = false
                if (".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*).*".toRegex().matches(it.name)) {
                    val name = ".*§(?:\\d|\\w)+Spawned by: §(?:\\d|\\w)(\\w*)".toRegex().find(it.name)?.groupValues?.get(1)
                    contain = it2 == name
                }
                contain
            }
            val neededHealth = when (baseMaxHealth) {
                625 -> 125f // t1
                1100 -> 220f // t2
                1800 -> 360f // t3
                2400 -> 480f // t4
                else -> 600f // t5
            }
            val canUseSteak = health <= neededHealth
            val color = if (canUseSteak && config.changeColorWhenCanSteak) config.steakColor.toChromaColor().withAlpha(config.withAlpha) else config.highlightColor.toChromaColor().withAlpha(config.withAlpha)
            /* val shouldRender = when (!containUser) {
                 true -> config.highlightOthers && isEnabled() && taggedEntityList.contains(this) && isNPC()
                 false -> config.highlightOwnBoss && isEnabled() && isNPC() || containCoop
             }*/
            if (containUser && taggedEntityList.contains(this.entityId)){
                taggedEntityList.remove(this.entityId)
            }
            val shouldRender = if (config.highlightOwnBoss && containUser && isNPC()) true
            else if (config.highlightOthers && taggedEntityList.contains(this.entityId) && isNPC()) true
            else config.coopsBossHighlight.highlight && containCoop && isNPC()

            RenderLivingEntityHelper.setEntityColor(this, color) { shouldRender }
            RenderLivingEntityHelper.setNoHurtTime(this) { shouldRender }
            if (shouldRender)
                entityList.add(this)
        }
    }

    @SubscribeEvent
    fun onEntityHit(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.clickedEntity !is EntityOtherPlayerMP) return
        if (!event.clickedEntity.isNPC()) return
        event.clickedEntity.getAllNameTagsInRadiusWith("Spawned by").forEach {
            if (it.name.contains(username)) return
            if (!taggedEntityList.contains(event.clickedEntity.entityId)) {
                taggedEntityList.add(event.clickedEntity.entityId)
            }

        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entityList.contains(entity)) {
            entityList.remove(entity)
        }
        if (taggedEntityList.contains(entity.entityId)) {
            taggedEntityList.remove(entity.entityId)
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Pre<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.seeThrough) return
        if (entityList.contains(event.entity) && LocationUtils.canSee(LocationUtils.playerEyeLocation(), event.entity.getLorenzVec())) {
            GlStateManager.disableDepth()
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Post<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.seeThrough) return
        if (entityList.contains(event.entity) && LocationUtils.canSee(LocationUtils.playerEyeLocation(), event.entity.getLorenzVec())) {
            GlStateManager.enableDepth()
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return
        if (config.bloodIchor.renderBeam)
            entityList.filterIsInstance<EntityArmorStand>().forEach {
                if (it.isEntityAlive) {
                    event.drawWaypointFilled(it.position.toLorenzVec().add(0, -2, 0), config.bloodIchor.color.toChromaColor(),
                        seeThroughBlocks = false,
                        beacon = true)
                }
            }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (!isEnabled()) return
        entityList.clear()
        taggedEntityList.clear()
    }

    fun isEnabled() = RiftAPI.inRift()
}