package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniLivingEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

/**
 * Fires once per tick per entity, to check what opacity we should hide the entity with.
 * Requires [EntityTransparencyFeatureActiveEvent] set to active.
 */
@PrimaryFunction("onEntityTransparencyTick")
class EntityTransparencyTickEvent<T : LivingEntity>(
    override val entity: T,
) : GenericSkyHanniEvent<T>(entity.javaClass), SkyHanniLivingEntityEvent<T> {
    var newTransparency: Int? = null
}
