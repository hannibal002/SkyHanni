package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.entity.LivingEntity

/**
 * This event is fired each tick that an entity of the specified type moved.
 * @param entity repreesnts the entity being tracked in the given move event
 * @param oldLocation is the entity's location in the prior tick
 * @param newLocation is the entity's current location as of the tick the event fired
 * @param distance is the amount of distance the entity moved between oldLocation and newLocation
 */
class EntityMoveEvent<T : LivingEntity>(
    val entity: T,
    val oldLocation: LorenzVec,
    val newLocation: LorenzVec,
    val distance: Double,
) : GenericSkyHanniEvent<T>(entity.javaClass)
