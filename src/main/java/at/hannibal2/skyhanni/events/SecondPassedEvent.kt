package at.hannibal2.skyhanni.events

class SecondPassedEvent(private val totalSeconds: Int) : LorenzEvent() {
    fun repeatSeconds(i: Int) = i % totalSeconds
}
