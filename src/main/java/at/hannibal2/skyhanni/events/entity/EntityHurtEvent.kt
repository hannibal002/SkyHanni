package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

@Thread(NETWORK)
class EntityHurtEvent<T : Entity>(val entity: T, val source: DamageSource, val amount: Float) : GenericSkyHanniEvent<T>(entity.javaClass)
