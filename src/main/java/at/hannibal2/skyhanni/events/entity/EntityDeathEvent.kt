package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onEntityDeath")
class EntityDeathEvent<T : LivingEntity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
