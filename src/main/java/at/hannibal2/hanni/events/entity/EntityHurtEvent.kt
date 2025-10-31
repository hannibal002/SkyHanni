package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity
import net.minecraft.util.DamageSource

class EntityHurtEvent<T : Entity>(val entity: T, val source: DamageSource, val amount: Float) : GenericHanniEvent<T>(entity.javaClass)
