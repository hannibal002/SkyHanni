package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorEntityData

@Thread(RENDER)
class DamageIndicatorDetectedEvent(val entityData: DamageIndicatorEntityData) : SkyHanniEvent()
