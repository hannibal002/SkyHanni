package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.Entity
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class EntityMovementHelper {

    companion object {
        private val entityLocation = mutableMapOf<Entity, LorenzVec>()

        fun addToTrack(entity: Entity) {
            if (entity !in entityLocation) {
                entityLocation[entity] = entity.getLorenzVec()
            }
        }
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return

        for (entity in entityLocation.keys) {
            if (entity.isDead) continue

            val newLocation = entity.getLorenzVec()
            val oldLocation = entityLocation[entity]!!
            val distance = newLocation.distance(oldLocation)
            if (distance > 0.01) {
                entityLocation[entity] = newLocation
                EntityMoveEvent(entity).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        entityLocation.clear()
    }
}