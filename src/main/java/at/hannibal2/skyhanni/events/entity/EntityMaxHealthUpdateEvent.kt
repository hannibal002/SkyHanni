package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.world.entity.LivingEntity

@Thread(RENDER)
class EntityMaxHealthUpdateEvent(val entity: LivingEntity, val maxHealth: Int) : SkyHanniEvent()
