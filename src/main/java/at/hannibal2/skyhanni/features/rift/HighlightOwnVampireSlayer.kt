package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
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
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightOwnVampireSlayer {

    private val config get() = SkyHanniMod.feature.rift.vampireSlayerFeatures
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private val taggedEntityList = mutableListOf<EntityOtherPlayerMP>()
    private val username = Minecraft.getMinecraft().session.username
    private val bloodIchorTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzAzNDA5MjNhNmRlNDgyNWExNzY4MTNkMTMzNTAzZWZmMTg2ZGIwODk2ZTMyYjY3MDQ5MjhjMmEyYmY2ODQyMiJ9fX0="
    private val killerSpringTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzdmN2E3YmM4YWM4NmYyM2NhN2JmOThhZmViNzY5NjAyMjdlMTgzMmZlMjA5YTMwMjZmNmNlYjhiZGU3NGY1NCJ9fX0="

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        val start = LocationUtils.playerLocation()
        if (config.highlightOwnBoss || config.highlightOthers) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach { it.process() }
        }
        if (config.bloodIchor.highlight || config.killerSpring.highlight) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>().forEach { stand ->
                if (stand.hasSkullTexture(bloodIchorTexture) || stand.hasSkullTexture(killerSpringTexture)) {
                    val vec = stand.position.toLorenzVec()
                    val distance = start.distance(vec)
                    val color = if (stand.hasSkullTexture(bloodIchorTexture)) config.bloodIchor.color.toChromaColor().withAlpha(config.withAlpha)
                    else if (stand.hasSkullTexture(killerSpringTexture)) config.killerSpring.color.toChromaColor().withAlpha(config.withAlpha)
                    else LorenzColor.WHITE.toColor().withAlpha(config.withAlpha)
                    RenderLivingEntityHelper.setEntityColor(
                        stand,
                        color
                    ) { isEnabled() && distance <= 20 }
                }
            }
        }
    }

    private fun EntityOtherPlayerMP.process() {
        val start = LocationUtils.playerLocation()
        val vec = position.toLorenzVec()
        val distance = start.distance(vec)
        val other = taggedEntityList.contains(this)
        if (name != "Bloodfiend ") return
        if (config.twinClawsTitle) {
            getAllNameTagsInRadiusWith("TWINCLAWS").forEach { stand ->
                if (".*(?:§(?:\\d|\\w))+TWINCLAWS (?:§(?:\\w|\\d))+([0-9.,]+s).*".toRegex().matches(stand.name)) {
                    val containUser = getAllNameTagsInRadiusWith("Spawned by").any { it.name.contains(username) }
                    if (containUser) {
                        val title = ".*(?:§(?:\\d|\\w))+TWINCLAWS (?:§(?:\\w|\\d))+([0-9.,]+s).*".toRegex().find(stand.name)?.groupValues?.get(1)
                        //I modified sendTitle to take a heightModifier, so the title don't overlap with impels and others titles
                        TitleUtils.sendTitle("§6TWINCLAWS $title", 150, 2.4)
                    }
                }
            }
        }
        getAllNameTagsInRadiusWith("Spawned by").forEach {
            val containUser = it.name.contains(username)
            val neededHealth = when (baseMaxHealth) {
                625 -> 125f // t1
                1100 -> 220f // t2
                1800 -> 360f // t3
                2400 -> 480f // t4
                else -> 600f // t5
            }
            val canUseSteak = health <= neededHealth
            val color = if (canUseSteak && config.changeColorWhenCanSteak) config.steakColor.toChromaColor().withAlpha(config.withAlpha) else config.highlightColor.toChromaColor().withAlpha(config.withAlpha)
            val shouldRender = when (other) {
                true -> config.highlightOthers && isEnabled() && it.name.contains("Spawned by") && !containUser && distance <= 20 && isNPC()
                false -> config.highlightOwnBoss && isEnabled() && containUser && distance <= 20 && isNPC()
            }
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
            if (!it.name.contains(username)) {
                if (!taggedEntityList.contains(event.clickedEntity))
                    taggedEntityList.add(event.clickedEntity)
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
        if (taggedEntityList.contains(entity)) {
            taggedEntityList.remove(entity)
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Pre<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.seeTrough) return
        if (entityList.contains(event.entity) && LocationUtils.canSee(LocationUtils.playerEyeLocation(), event.entity.getLorenzVec())) {
            GlStateManager.disableDepth()
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Post<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.seeTrough) return
        if (entityList.contains(event.entity) && LocationUtils.canSee(LocationUtils.playerEyeLocation(), event.entity.getLorenzVec())) {
            GlStateManager.enableDepth()
        }
    }

    fun isEnabled() = RiftAPI.inRift()
}