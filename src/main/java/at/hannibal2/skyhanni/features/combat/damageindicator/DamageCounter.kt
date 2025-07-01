package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import java.util.LinkedList

class DamageCounter {

    var currentDamage = 0L
    var currentHealing = 0L
    var oldDamages = LinkedList<OldDamage>()
    var firstTick = SimpleTimeMark.farPast()
}
