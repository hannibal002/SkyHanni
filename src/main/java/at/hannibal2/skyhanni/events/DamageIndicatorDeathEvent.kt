package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorEntityData
import net.minecraft.entity.EntityLivingBase

class DamageIndicatorDeathEvent(val entity: EntityLivingBase, val data: DamageIndicatorEntityData) : HanniEvent()

