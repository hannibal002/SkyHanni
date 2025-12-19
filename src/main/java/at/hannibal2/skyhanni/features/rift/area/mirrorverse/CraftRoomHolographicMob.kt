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
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

// TODO fix looking at direction, slime size, helmet/skull of zombie
@SkyHanniModule
object CraftRoomHolographicMob {

    private val config get() = SkyHanniMod.feature.rift.area.mirrorverse.craftingRoom
    private val craftRoomArea = AABB(
        -108.0, 58.0, -106.0,
        -117.0, 51.0, -128.0,
    )
    private val entityToHolographicEntity = mapOf(
        Zombie::class.java to HolographicEntities.zombie,
        Slime::class.java to HolographicEntities.slime,
        CaveSpider::class.java to HolographicEntities.caveSpider,
    )

    private var holograms = mapOf<HolographicEntities.HolographicEntity<out LivingEntity>, String?>()
    private var enabled = false

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        enabled = config.enabled && craftRoomArea.isPlayerInside()
        if (!enabled) return

        val map = mutableMapOf<HolographicEntities.HolographicEntity<out LivingEntity>, String?>()
        for (entity in EntityUtils.getEntitiesNextToPlayer<LivingEntity>(25.0)) {
            if (entity is Player) continue
            val holographicEntity = entityToHolographicEntity[entity::class.java] ?: continue

            val currentLocation = entity.getLorenzVec()
            if (!craftRoomArea.isInside(currentLocation)) continue
            val previousLocation = LorenzVec(entity.xo, entity.yo, entity.zo) // used to interpolate movement

            // we currently don't rotate the body so head rotations looked very weird
            val instance = holographicEntity.instance(previousLocation.mirror(), 0f)
            instance.isChild = entity.isBaby
            instance.moveTo(currentLocation.mirror(), 0f)
            map[instance] = entity.display()
        }
        holograms = map
    }

    private fun LivingEntity.display() = buildString {
        if (config.showName) {
            val mobName = displayName.formattedTextCompat()
            append("§a$mobName ")
        }
        if (config.showHealth) {
            append("§c${findHealthReal().roundTo(1)}♥")
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
    fun onPlayerRender(event: CheckRenderEntityEvent<RemotePlayer>) {
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
