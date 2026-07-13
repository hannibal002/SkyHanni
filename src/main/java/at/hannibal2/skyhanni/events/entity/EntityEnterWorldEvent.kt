package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.Entity

/**
 * Fired when an entity is added to the client world.
 *
 * Fired on the main client thread via a Mixin into `ClientLevel.addEntity`.
 *
 * Use this to detect entities appearing in the world, such as mobs spawning or projectiles
 * being created. The type parameter [T] allows filtering for a specific entity type.
 *
 * @param T the type of entity
 * @param entity the entity that was added to the world
 */
class EntityEnterWorldEvent<T : Entity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
