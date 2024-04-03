package at.hannibal2.skyhanni.events

class LorenzTickEvent(private val tick: Int) : LorenzEvent() {

    fun isMod(i: Int, seed: Int = 0) = (tick + seed) % i == 0

    fun repeatSeconds(i: Int, seed: Int = 0) = isMod(i * 20, seed)
}
