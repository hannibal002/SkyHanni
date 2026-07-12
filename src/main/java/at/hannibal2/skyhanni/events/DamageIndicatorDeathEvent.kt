package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorEntityData
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.world.entity.LivingEntity

@Thread(RENDER)
class DamageIndicatorDeathEvent(val entity: LivingEntity, val data: DamageIndicatorEntityData) : SkyHanniEvent()

