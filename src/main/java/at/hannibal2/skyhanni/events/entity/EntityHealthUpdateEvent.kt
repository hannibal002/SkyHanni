package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.world.entity.LivingEntity

@Thread(RENDER)
class EntityHealthUpdateEvent(val entity: LivingEntity, val health: Int) : SkyHanniEvent()
