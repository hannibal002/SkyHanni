package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import kotlin.time.Duration

@Deprecated(
    "LorenzTickEvent ticks with double speed",
    ReplaceWith("NeaTickEvent", "at.hannibal2.skyhanni.events.NeaTickEvent")
)
class LorenzTickEvent constructor(private val tick: Int) : LorenzEvent() {
    fun isMod(i: Int) = tick % i == 0

    fun repeatSeconds(i: Int) = isMod(i * 20)
}

data class NeaTickEvent(val tick: Int) : LorenzEvent() {
    fun isMod(order: Int, seed: Int = 0) = (tick % order) == (seed % order)
    fun repeatEvery(duration: Duration, seed: Int = 0) = isMod(duration.inWholeTicks, seed)
    fun repeatSeconds(seconds: Int) = isMod(seconds * 20)
}
