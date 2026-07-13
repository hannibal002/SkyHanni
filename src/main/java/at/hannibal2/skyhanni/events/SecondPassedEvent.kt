package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@Thread(RENDER)
@PrimaryFunction("onSecondPassed")
class SecondPassedEvent(private val totalSeconds: Int) : SkyHanniEvent() {
    fun repeatSeconds(i: Int) = totalSeconds % i == 0
}
