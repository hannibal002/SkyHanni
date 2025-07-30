package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.EntityLivingBase

/**
 * Fires once per tick per entity, to check what opacity we should hide the entity with.
 * Requires [EntityOpacityActiveEvent] set to active.
 */
class EntityOpacityEvent<T : EntityLivingBase>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass) {

    var opacity: Int? = null
}
