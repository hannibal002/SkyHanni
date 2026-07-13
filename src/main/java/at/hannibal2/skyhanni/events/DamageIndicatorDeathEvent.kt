package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorEntityData
import net.minecraft.world.entity.LivingEntity

class DamageIndicatorDeathEvent(val entity: LivingEntity, val data: DamageIndicatorEntityData) : SkyHanniEvent()

