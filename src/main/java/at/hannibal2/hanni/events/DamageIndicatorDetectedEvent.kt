package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorEntityData

class DamageIndicatorDetectedEvent(val entityData: DamageIndicatorEntityData) : HanniEvent()
