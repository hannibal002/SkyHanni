package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

/**
 * Event that is called when an entity's max health is updated.
 *
 * @property entity The entity whose max health was updated.
 * @property maxHealth The new max health of the entity.
 */
@PrimaryFunction("onEntityMaxHealthUpdate")
data class EntityMaxHealthUpdateEvent<T : LivingEntity>(
    val entity: T,
    val maxHealth: Int
) : GenericSkyHanniEvent<T>(entity.javaClass)
