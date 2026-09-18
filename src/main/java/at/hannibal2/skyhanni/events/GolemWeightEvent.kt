package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Sent once the weight of a finished protector fight is known. The zealot count it needs only
 * arrives after [EndBossFightEndEvent], so anything depending on the weight has to wait for this
 * rather than reading it at the end of the fight.
 */
@PrimaryFunction("onGolemWeight")
class GolemWeightEvent(val weight: Double) : SkyHanniEvent()
