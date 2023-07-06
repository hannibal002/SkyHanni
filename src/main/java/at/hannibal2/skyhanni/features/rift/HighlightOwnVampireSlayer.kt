package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
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

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return
        val name = Minecraft.getMinecraft().session.username
        val start = LocationUtils.playerLocation()
        val entities = Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>()
        for (entity in entities) {
            val entityPosition = entity.position
            val vec = entityPosition.toLorenzVec()
            val distance = start.distance(vec)
            if (entity.hasNameTagWith(2, name)) {
                val health = when (entity.maxHealth) {
                    625f -> 125f
                    1100f -> 220f
                    2400f -> 480f
                    3000f -> 600f
                    else -> 0f
                }
                val canUseSteak = entity.health <= health
                val color = if (canUseSteak && config.changeColorWhenCanSteak) config.steakColor.toChromaColor().withAlpha(config.withAlpha) else config.highlightColor.toChromaColor().withAlpha(config.withAlpha)

                val shouldRender = isEnabled() && entity.hasNameTagWith(2, name) && distance <= 20 && entity.isNPC()

                RenderLivingEntityHelper.setEntityColor(entity, color) { shouldRender }
                RenderLivingEntityHelper.setNoHurtTime(entity) { shouldRender }
                if (shouldRender)
                    entityList.add(entity)
            }
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (!isEnabled()) return
        if (entityList.contains(event.entity)) {
            entityList.remove(event.entity)
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Pre<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (entityList.contains(event.entity) && config.seeTrough) {
            GlStateManager.disableDepth()
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Post<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (entityList.contains(event.entity) && config.seeTrough) {
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