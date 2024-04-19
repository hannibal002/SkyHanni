package at.hannibal2.skyhanni.events

open class BitsUpdateEvent(val bits: Int, val bitsAvailable: Int) : LorenzEvent() {
    class BitsGain(bits: Int, bitsAvailable: Int) : BitsUpdateEvent(bits, bitsAvailable)
    class BitsSpent(bits: Int, bitsAvailable: Int) : BitsUpdateEvent(bits, bitsAvailable)
    class BitsAvailableGained(bits: Int, bitsAvailable: Int) : BitsUpdateEvent(bits, bitsAvailable)
}
