package at.hannibal2.hanni.features.combat.damageindicator

import at.hannibal2.hanni.utils.SimpleTimeMark
import java.util.LinkedList

class DamageCounter {

    var currentDamage = 0L
    var currentHealing = 0L
    var oldDamages = LinkedList<OldDamage>()
    var firstTick = SimpleTimeMark.farPast()
}
