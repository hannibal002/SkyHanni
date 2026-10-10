package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.LivingEntity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onEntityHealthUpdate")
class EntityHealthUpdateEvent<T : LivingEntity>(val entity: T, val health: Int) :
    GenericSkyHanniEvent<T>(entity.javaClass)
