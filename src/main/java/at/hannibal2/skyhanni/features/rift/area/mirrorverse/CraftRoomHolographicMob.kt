package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityCaveSpider
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import kotlin.math.abs

// TODO fix looking at direction, slime size, helmet/skull of zombie
@SkyHanniModule
object CraftRoomHolographicMob {

    private val config get() = SkyHanniMod.feature.rift.area.mirrorverse.craftingRoom
    private val craftRoomArea = AxisAlignedBB(
        -108.0, 58.0, -106.0,
        -117.0, 51.0, -128.0,
    )
    private val entityToHolographicEntity = mapOf(
        EntityZombie::class.java to HolographicEntities.zombie,
        EntitySlime::class.java to HolographicEntities.slime,
        EntityCaveSpider::class.java to HolographicEntities.caveSpider,
    )

    private var hologramMap: MutableMap<HolographicEntities.HolographicEntity<out EntityLivingBase>, String?> = mutableMapOf()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        if (!isEnabled()) return
        val newMap: MutableMap<HolographicEntities.HolographicEntity<out EntityLivingBase>, String?> = mutableMapOf()
        for (theMob in EntityUtils.getEntitiesNextToPlayer<EntityLivingBase>(25.0)) {
            if (theMob is EntityPlayer) continue

            val mobPos = theMob.getLorenzVec()
            val lastTickMobPos = LorenzVec(theMob.lastTickPosX, theMob.lastTickPosY, theMob.lastTickPosZ) // used to interpolate movement
            if (!craftRoomArea.isInside(mobPos) || !craftRoomArea.isInside(lastTickMobPos)) continue
            val holographicMobPos = getMirroredPos(lastTickMobPos)
            val mirroredPos = getMirroredPos(mobPos)

            val displayString = buildString {
                val mobName = theMob.displayName.formattedText
                if (config.showName) {
                    append("§a$mobName ")
                }
                if (config.showHealth) {
                    append("§c${theMob.health.roundTo(1)}♥")
                }
            }.trim()

            val mob = entityToHolographicEntity[theMob::class.java] ?: continue

            val instance = mob.instance(holographicMobPos, -theMob.rotationYaw)

            instance.isChild = theMob.isChild
            instance.moveTo(mirroredPos, -theMob.rotationYaw)

            newMap[instance] = displayString
        }
        hologramMap = newMap
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        for (entity in hologramMap.entries) {
            val mob = entity.key
            val string = entity.value
            event.renderHolographicEntity(mob)

            if (string != null) {
                event.drawString(mob.position.add(y = mob.entity.eyeHeight + .5), string)
            }
        }
    }

    @HandleEvent(receiveCancelled = true, onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerRender(event: CheckRenderEntityEvent<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.hidePlayers) return

        val entity = event.entity
        if (craftRoomArea.isInside(entity.getLorenzVec())) {
            event.cancel()
        }
    }

    private fun getMirroredPos(pos: LorenzVec): LorenzVec {
        val wallZ = -116.5
        val dist = abs(pos.z - wallZ)
        return pos.add(z = dist * 2)
    }

    private fun isEnabled() = RiftApi.inRift() && config.enabled && craftRoomArea.isInside(playerLocation())
}
