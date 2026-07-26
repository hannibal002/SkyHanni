package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

/**
 * Event that is fired when text associated with an entity is updated.
 * This includes both text display entities and armor stand custom names.
 * Do note that event.entity.customName is NOT equivalent to event.newName for text display entities.
 * Only fires on SkyBlock.
 *
 * @property entity The entity whose associated text was updated.
 * @property newName The new text associated with the entity, or null if the text was removed.
 */
@PrimaryFunction("onEntityTextUpdate")
data class EntityTextUpdateEvent(
    val entity: Entity,
    val newName: Component?,
) : SkyHanniEvent()

/**
 * Event that is fired when a text display entity or an armor stand with a custom name is removed from the world.
 * Only fires on SkyBlock.
 *
 * @property entity The entity that was removed.
 */
@PrimaryFunction("onEntityTextRemoved")
data class EntityTextRemovedEvent(
    val entity: Entity,
) : SkyHanniEvent()
