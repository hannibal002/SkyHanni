package at.hannibal2.skyhanni.events

class LorenzTickEvent(private val tick: Int) : LorenzEvent() {
    fun isMod(i: Int) = tick % i == 0

    fun repeatSeconds(i: Int) = isMod(i * 20)
}