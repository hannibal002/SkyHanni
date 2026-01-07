package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.world.entity.LivingEntity

class EntityMaxHealthUpdateEvent(val entity: LivingEntity, val maxHealth: Int) : SkyHanniEvent()
