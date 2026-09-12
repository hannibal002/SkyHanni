package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.LivingEntity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onEntityDeath")
class EntityDeathEvent<T : LivingEntity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
