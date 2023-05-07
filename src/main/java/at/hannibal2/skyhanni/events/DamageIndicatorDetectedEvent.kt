package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.damageindicator.EntityData

class DamageIndicatorDetectedEvent(val entityData: EntityData) : LorenzEvent()