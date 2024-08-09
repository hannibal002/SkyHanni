package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.entity.Entity
import net.minecraft.util.DamageSource

class EntityHurtEvent(val entity: Entity, val source: DamageSource, val amount: Float) : SkyHanniEvent()
