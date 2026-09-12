package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.core.Holder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute

/**
 * Event that is called when an entity's attribute is updated.
 * Like max health, movement speed, absorption etc.
 *
 * @param T The type of the entity whose attribute was updated.
 * @property entity The entity whose attribute was updated.
 * @property attribute The attribute that was updated.
 */
@PrimaryFunction("onEntityAttributeUpdate")
data class EntityAttributeUpdateEvent<T : LivingEntity>(
    val entity: T,
    val attribute: Holder<Attribute>,
) : GenericSkyHanniEvent<T>(entity.javaClass)
