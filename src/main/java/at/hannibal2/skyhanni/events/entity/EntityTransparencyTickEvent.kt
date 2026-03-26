package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.LivingEntity

/**
 * Fires once per tick per entity, to check what opacity we should hide the entity with.
 * Requires [EntityTransparencyActiveEvent] set to active.
 */
class EntityTransparencyTickEvent<T : LivingEntity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass) {
    var newTransparency: Int? = null
}
