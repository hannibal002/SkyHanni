package at.hannibal2.skyhanni.features.damageindicator

class DamageCounter {

    var currentDamage = 0L
    var currentHealing = 0L
    var oldDamages = mutableListOf<OldDamage>()
    var firstTick = 0L

}