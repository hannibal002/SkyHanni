package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.entity.EntityLivingBase

class EntityMaxHealthUpdateEvent(val entity: EntityLivingBase, val maxHealth: Int) : HanniEvent()
