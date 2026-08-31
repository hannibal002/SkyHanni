package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.Entity

@Deprecated("Use EntityLeaveWorldEvent instead", ReplaceWith("EntityLeaveWorldEvent"))
typealias EntityRemovedEvent<T> = EntityLeaveWorldEvent<T>

/**
 * Fired when an entity is removed from the client world.
 *
 * Fired on the main client thread via a Mixin into `TransientEntitySectionManager.Callback.onRemove`.
 *
 * Covers every removal reason: despawning, dying, leaving the tracking range and chunk unloading.
 * The reason is not part of the event, so this is not a death detection. The type parameter [T]
 * allows filtering for a specific entity type.
 *
 * @param T the type of entity
 * @param entity the entity that was removed from the world
 */
@PrimaryFunction("onEntityLeaveWorld")
class EntityLeaveWorldEvent<T : Entity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
