package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onTick")
class HanniTickEvent(private val tick: Int) : HanniEvent() {

    fun isMod(i: Int, offset: Int = 0) = (tick + offset) % i == 0

    /**
     * Use of this method is discouraged, use [SecondPassedEvent] instead.
     * Only use if very needed.
     */
    fun repeatSeconds(i: Int, offset: Int = 0) = isMod(i * 20, offset)
}
