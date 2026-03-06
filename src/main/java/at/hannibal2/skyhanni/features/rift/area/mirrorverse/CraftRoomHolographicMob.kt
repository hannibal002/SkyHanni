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
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.filterNotClass
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.monster.spider.CaveSpider
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

// TODO fix looking at direction, slime size, helmet/skull of zombie
@SkyHanniModule
object CraftRoomHolographicMob {

    private val config get() = SkyHanniMod.feature.rift.area.mirrorverse.craftingRoom
    private val enabled get() = config.enabled && craftRoomArea.isPlayerInside()

    private val craftRoomArea = AABB(
        -108.0, 58.0, -106.0,
        -117.0, 51.0, -128.0,
    )
    private val entityToHolographicEntity get() = HolographicEntities.getFilteredEntityHoloBases(
        Zombie::class,
        Slime::class,
        CaveSpider::class,
    )

    private var holograms = mapOf<Int, Pair<HolographicEntities.HolographicEntity<out LivingEntity>, String?>>()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        if (!enabled) {
            holograms = emptyMap()
            return
        }

        val nonPlayerEntities = EntityUtils.getEntitiesNearby<LivingEntity>(25.0).filterNotClass(Player::class)
        val newHolograms = mutableMapOf<Int, Pair<HolographicEntities.HolographicEntity<out LivingEntity>, String?>>()

        for (entity in nonPlayerEntities) {
            val holographicBase = entityToHolographicEntity[entity::class] ?: continue
            val currentLocation = entity.getLorenzVec()
            if (!craftRoomArea.isInside(currentLocation)) continue

            val existing = holograms[entity.id]
            val instance = if (existing != null) {
                existing.first.also { it.moveTo(currentLocation.mirror(), 0f) }
            } else {
                val previousLocation = LorenzVec(entity.xo, entity.yo, entity.zo)
                val new = holographicBase.instance(previousLocation.mirror(), 0f) ?: continue
                new.isChild = entity.isBaby
                new.moveTo(currentLocation.mirror(), 0f)
                new
            }

            newHolograms[entity.id] = instance to entity.display()
        }

        holograms = newHolograms
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
        holograms.values.forEach { (mob, string) ->
            event.renderHolographicEntity(mob)
            event.drawString(mob.position.add(y = mob.entity.eyeHeight + .5), string.orEmpty())
        }
    }

    @HandleEvent(receiveCancelled = true, onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerRender(event: CheckRenderEntityEvent<RemotePlayer>) {
        if (!enabled || !config.hidePlayers) return
        event.cancel()
    }

    private const val WALL_Z = -116.5
    private fun LorenzVec.mirror(): LorenzVec {
        require(z <= WALL_Z) { "mirror() assumes z <= WALL_Z, z was ${z.roundTo(1)} instead" }
        val dist = WALL_Z - z
        return add(z = dist * 2)
    }
}
