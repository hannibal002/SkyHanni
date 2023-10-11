package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.damageindicator.EntityData

class BossHealthChangeEvent(val entityData: EntityData, val lastHealth: Long, val health: Long, val maxHealth: Long) :
    LorenzEvent()