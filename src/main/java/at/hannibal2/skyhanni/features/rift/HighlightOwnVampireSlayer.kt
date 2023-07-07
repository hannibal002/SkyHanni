package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightOwnVampireSlayer {

    private val config get() = SkyHanniMod.feature.rift.highlightOwnSlayer
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private val taggedEntityList = mutableListOf<EntityOtherPlayerMP>()
    private val username = Minecraft.getMinecraft().session.username

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return
        Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach { it.process() }
        taggedEntityList.forEach { it.process(true) }
    }

    private fun EntityOtherPlayerMP.process(other: Boolean = false) {
        val start = LocationUtils.playerLocation()
        val vec = position.toLorenzVec()
        val distance = start.distance(vec)
        if (hasNameTagWith(3, "Spawned by")) {
            val neededHealth = when (maxHealth) {
                625f -> 125f // t1
                1100f -> 220f // t2
                1800f -> 360f // t3
                2400f -> 480f // t4
                else -> 600f // t5
            }
            val canUseSteak = health <= neededHealth
            val color = if (canUseSteak && config.changeColorWhenCanSteak) config.steakColor.toChromaColor().withAlpha(config.withAlpha) else config.highlightColor.toChromaColor().withAlpha(config.withAlpha)
            val shouldRender = when (other) {
                true -> taggedEntityList.contains(this) && config.highlightOthers && isEnabled() && hasNameTagWith(2, "Spawned by") && !hasNameTagWith(2, username) && distance <= 20 && isNPC()
                false -> isEnabled() && hasNameTagWith(2, username) && distance <= 20 && isNPC()
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
        if (event.clickedEntity.hasNameTagWith(2, username)) return
        if (!taggedEntityList.contains(event.clickedEntity))
            taggedEntityList.add(event.clickedEntity)
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
        if (entityList.contains(event.entity)) {
            GlStateManager.disableDepth()
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Post<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.seeTrough) return
        if (entityList.contains(event.entity)) {
            GlStateManager.enableDepth()
        }
    }

    private fun EntityLivingBase.hasNameTagWith(
            y: Int,
            contains: String,
            debugRightEntity: Boolean = false,
            inaccuracy: Double = 1.6,
            debugWrongEntity: Boolean = false,
    ): Boolean {
        return getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null
    }

    private fun EntityLivingBase.getNameTagWith(
            y: Int,
            contains: String,
            debugRightEntity: Boolean = false,
            inaccuracy: Double = 1.6,
            debugWrongEntity: Boolean = false,
    ): EntityArmorStand? {
        val center = getLorenzVec().add(0, y, 0)
        val a = center.add(-inaccuracy, -inaccuracy - 3, -inaccuracy).toBlocPos()
        val b = center.add(inaccuracy, inaccuracy + 3, inaccuracy).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.find {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                LorenzUtils.consoleLog("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                LorenzUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                LorenzUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                LorenzUtils.consoleLog("accuracy: " + it.getLorenzVec().subtract(center).printWithAccuracy(3))
            }
            result
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}