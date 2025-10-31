package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorEntityData

class BossHealthChangeEvent(
    val entityData: DamageIndicatorEntityData,
    val lastHealth: Long,
    val health: Long,
    val maxHealth: Long,
) : HanniEvent()
