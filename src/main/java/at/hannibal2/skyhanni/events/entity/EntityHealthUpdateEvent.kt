package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.entity.EntityLivingBase

class EntityHealthUpdateEvent(val entity: EntityLivingBase, val health: Int) : HanniEvent()
