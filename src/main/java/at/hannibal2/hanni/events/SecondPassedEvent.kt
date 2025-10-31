package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onSecondPassed")
class SecondPassedEvent(private val totalSeconds: Int) : HanniEvent() {
    fun repeatSeconds(i: Int) = totalSeconds % i == 0
}
