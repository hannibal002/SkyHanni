package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.Entity

/**
 * Event that is called when an entity is removed from the world.
 * This event is called for all entities, not just living entities.
 * If you need an event for when an entity explicitly dies, use [EntityDeathEvent].
 *
 * @param T The type of the entity that was removed.
 * @property entity The entity that was removed.
 */
@PrimaryFunction("onEntityRemoved")
class EntityRemovedEvent<T : Entity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
