package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.world.entity.LivingEntity

class EntityHealthUpdateEvent(val entity: LivingEntity, val health: Int) : SkyHanniEvent()
