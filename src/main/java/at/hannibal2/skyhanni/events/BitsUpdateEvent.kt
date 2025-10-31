package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent

open class BitsUpdateEvent(val bits: Int, val bitsAvailable: Int, val difference: Int) : HanniEvent() {
    class BitsGain(bits: Int, bitsAvailable: Int, difference: Int) : BitsUpdateEvent(bits, bitsAvailable, difference)
    class BitsSpent(bits: Int, bitsAvailable: Int, difference: Int) : BitsUpdateEvent(bits, bitsAvailable, difference)
}

class BitsAvailableUpdateEvent(val bitsAvailable: Int) : HanniEvent()
