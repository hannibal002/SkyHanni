package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

/**
 * Event that is fired when a text display entity's custom name is updated.
 * This includes both text display and armor stand entities, as they can both have custom names.
 * Only Fires in Hypixel Skyblock
 *
 * @property entity The entity whose custom name was updated.
 * @property newName The new custom name of the entity, or null if the name was removed.
 */
@PrimaryFunction("onTextDisplayUpdate")
data class TextDisplayUpdateEvent(
    val entity: Entity,
    val newName: Component?,
) : SkyHanniEvent()

/**
 * Event that is fired when a text display entity is removed from the world.
 * This includes both text display and armor stand entities, as they can both have custom names.
 * Only Fires in Hypixel Skyblock
 *
 * @property entity The entity that was removed.
 */
@PrimaryFunction("onTextDisplayRemoved")
data class TextDisplayRemovedEvent(
    val entity: Entity,
) : SkyHanniEvent()
