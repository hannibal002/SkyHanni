package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.gameevent.TickEvent

class LorenzTickEvent(private val tick: Int, val eventPhase: TickEvent.Phase) : LorenzEvent() {
    fun isMod(i: Int) = tick % i == 0

    fun repeatSeconds(i: Int) = isMod(i * 20)
}
