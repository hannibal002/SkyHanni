package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorEntityData
import net.minecraft.entity.EntityLivingBase

class DamageIndicatorDeathEvent(val entity: EntityLivingBase, val data: DamageIndicatorEntityData) : SkyHanniEvent()

