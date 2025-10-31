package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.EntityLivingBase

/**
 * Fires once per tick per entity, to check what opacity we should hide the entity with.
 * Requires [EntityOpacityActiveEvent] set to active.
 */
// TODO rename to transparency
class EntityOpacityEvent<T : EntityLivingBase>(val entity: T) : GenericHanniEvent<T>(entity.javaClass) {

    var opacity: Int? = null
}
