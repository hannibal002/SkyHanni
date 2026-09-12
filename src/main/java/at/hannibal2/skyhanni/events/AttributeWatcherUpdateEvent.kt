package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.core.Holder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute

@PrimaryFunction("onAttributeWatcherUpdate")
data class AttributeWatcherUpdateEvent<T : LivingEntity>(
    val entity: T,
    val attribute: Holder<Attribute>,
) : GenericSkyHanniEvent<T>(entity.javaClass)
