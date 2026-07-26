package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

/**
 * Event that is called when an entity dies.
 * This event is only for entities which are actually killed, not removed.
 * use [EntityRemovedEvent] if you need a removed entity event.
 *
 * @param T The type of the entity that died.
 * @property entity The entity that died.
 */
@PrimaryFunction("onEntityDeath")
class EntityDeathEvent<T : LivingEntity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
