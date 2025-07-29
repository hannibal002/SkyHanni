package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorEntityData

class BossHealthChangeEvent(
    val entityData: DamageIndicatorEntityData,
    val lastHealth: Long,
    val health: Long,
    val maxHealth: Long,
) : SkyHanniEvent()
