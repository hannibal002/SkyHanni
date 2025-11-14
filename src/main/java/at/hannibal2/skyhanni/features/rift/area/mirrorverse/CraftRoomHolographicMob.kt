package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
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

    private var holograms = mapOf<HolographicEntities.HolographicEntity<out EntityLivingBase>, String?>()
    private var enabled = false

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        enabled = config.enabled && craftRoomArea.isPlayerInside()
        if (!enabled) return

        val map = mutableMapOf<HolographicEntities.HolographicEntity<out EntityLivingBase>, String?>()
        for (entity in EntityUtils.getEntitiesNextToPlayer<EntityLivingBase>(25.0)) {
            if (entity is EntityPlayer) continue
            val holographicEntity = entityToHolographicEntity[entity::class.java] ?: continue

            val currentLocation = entity.getLorenzVec()
            if (!craftRoomArea.isInside(currentLocation)) continue
            val previousLocation = LorenzVec(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ) // used to interpolate movement

            // we currently don't rotate the body so head rotations looked very weird
            val instance = holographicEntity.instance(previousLocation.mirror(), 0f)
            instance.isChild = entity.isChild
            instance.moveTo(currentLocation.mirror(), 0f)
            map[instance] = entity.display()
        }
        holograms = map
    }

    private fun EntityLivingBase.display() = buildString {
        if (config.showName) {
            val mobName = displayName.formattedText
            append("§a$mobName ")
        }
        if (config.showHealth) {
            append("§c${health.roundTo(1)}♥")
        }
    }.trim().takeIf { it.isNotEmpty() }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!enabled) return
        for ((mob, string) in holograms) {
            event.renderHolographicEntity(mob)

            string?.let {
                event.drawString(mob.position.add(y = mob.entity.eyeHeight + .5), it)
            }
        }
    }

    @HandleEvent(receiveCancelled = true, onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerRender(event: CheckRenderEntityEvent<EntityOtherPlayerMP>) {
        if (enabled && config.hidePlayers) {
            event.cancel()
        }
    }

    private const val WALL_Z = -116.5
    private fun LorenzVec.mirror(): LorenzVec {
        require(z <= WALL_Z) { "mirror() assumes z <= WALL_Z, z was ${z.roundTo(1)} instead" }
        val dist = WALL_Z - z
        return add(z = dist * 2)
    }
}
