package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
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
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach { it.process() }
    }

    private fun EntityOtherPlayerMP.process() {
        val start = LocationUtils.playerLocation()
        val vec = position.toLorenzVec()
        val distance = start.distance(vec)
        val other = taggedEntityList.contains(this)
        if (name != "Bloodfiend ") return
        getAllNameTags("Spawned by").forEach {
            val neededHealth = when (baseMaxHealth) {
                625 -> 125f // t1
                1100 -> 220f // t2
                1800 -> 360f // t3
                2400 -> 480f // t4
                else -> 600f // t5
            }
            val canUseSteak = health <= neededHealth
            val color = if (canUseSteak && config.changeColorWhenCanSteak) config.steakColor.toChromaColor().withAlpha(config.withAlpha) else config.highlightColor.toChromaColor().withAlpha(config.withAlpha)
            //println("name ${it.name}")
            val shouldRender = when (other) {
                true -> config.highlightOthers && isEnabled() && it.name.contains("Spawned by") && !it.name.contains(username) && distance <= 20 && isNPC()
                false -> isEnabled() && it.name.contains(username) && distance <= 20 && isNPC()
            }
            /*println("===")
            println("name: ${it.name}")
            println("shouldRender: $shouldRender")
            println("enabled: ${isEnabled()}")
            println("contain: ${it.name.contains(username)}")
            println("distance <= 20: ${distance <= 20}")
            println("isNPC: ${isNPC()}")
            println("taggedContain: ${taggedEntityList.contains(this)}")
            println("===")*/
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
        event.clickedEntity.getAllNameTags("Spawned by").forEach {
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

    /*
    Sometime the armorstand move by 1bloc on x and z so i had to do this
    Maybe there is another solution
     */
    private fun EntityLivingBase.getAllNameTags(
        contains: String,
        inaccuracy: Double = 5.0,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().add(0, 5, 0)
        val a = center.add(-5.0, -inaccuracy - 3, -5.0).toBlocPos()
        val b = center.add(5.0, inaccuracy + 3, 5.0).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.filter {
            val result = it.name.contains(contains)
            result
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}