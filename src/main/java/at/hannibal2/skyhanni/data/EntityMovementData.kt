package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EntityMovementData {

    companion object {
        private val entityLocation = mutableMapOf<Entity, LorenzVec>()

        fun addToTrack(entity: Entity) {
            if (entity !in entityLocation) {
                entityLocation[entity] = entity.getLorenzVec()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        addToTrack(Minecraft.getMinecraft().thePlayer)

        for (entity in entityLocation.keys) {
            if (entity.isDead) continue

            val newLocation = entity.getLorenzVec()
            val oldLocation = entityLocation[entity]!!
            val distance = newLocation.distance(oldLocation)
            if (distance > 0.01) {
                entityLocation[entity] = newLocation
                EntityMoveEvent(entity, oldLocation, newLocation, distance).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        entityLocation.clear()
    }
}
