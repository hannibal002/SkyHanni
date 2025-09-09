package at.hannibal2.skyhanni.features.rift.area.mirrorverse import at.hannibal2.skyhanni.utils.compat.formattedTextCompat import at.hannibal2.skyhanni.utils.compat.findHealthReal

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
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.CaveSpiderEntity
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import kotlin.math.abs

// TODO fix looking at direction, slime size, helmet/skull of zombie
@SkyHanniModule
object CraftRoomHolographicMob {

    private val config get() = SkyHanniMod.feature.rift.area.mirrorverse.craftingRoom
    private val craftRoomArea = Box(
        -108.0, 58.0, -106.0,
        -117.0, 51.0, -128.0,
    )
    private var entitiesList = listOf<HolographicEntities.HolographicEntity<out LivingEntity>>()
    private val entityToHolographicEntity = mapOf(
        ZombieEntity::class.java to HolographicEntities.zombie,
        SlimeEntity::class.java to HolographicEntities.slime,
        CaveSpiderEntity::class.java to HolographicEntities.caveSpider,
    )

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        for (entity in entitiesList) {
            entity.moveTo(entity.position.up(.1), (entity.yaw + 5) % 360)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        entitiesList = emptyList()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for (theMob in EntityUtils.getEntitiesNextToPlayer<LivingEntity>(25.0)) {
            if (theMob is PlayerEntity) continue

            val mobPos = theMob.getLorenzVec()
            if (!craftRoomArea.isInside(mobPos)) continue

            val wallZ = -116.5
            val dist = abs(mobPos.z - wallZ)
            val holographicMobPos = mobPos.add(z = dist * 2)
            val displayString = buildString {
                val mobName = theMob.displayName.formattedTextCompat()
                if (config.showName) {
                    append("§a$mobName ")
                }
                if (config.showHealth) {
                    append("§c${theMob.findHealthReal().roundTo(1)}♥")
                }
            }.trim()

            val mob = entityToHolographicEntity[theMob::class.java] ?: continue

            val instance = mob.instance(holographicMobPos, -theMob.yaw)

            instance.isChild = theMob.isBaby

            event.renderHolographicEntity(instance)

            if (displayString.isNotEmpty()) {
                event.drawString(holographicMobPos.add(y = theMob.standingEyeHeight + .5), displayString)
            }

            entitiesList = entitiesList.editCopy { add(instance) }
        }
    }

    @HandleEvent(receiveCancelled = true, onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerRender(event: CheckRenderEntityEvent<OtherClientPlayerEntity>) {
        if (!config.hidePlayers) return

        val entity = event.entity
        if (craftRoomArea.isInside(entity.getLorenzVec())) {
            event.cancel()
        }
    }

    private fun isEnabled() = RiftApi.inRift() && config.enabled
}
