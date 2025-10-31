package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent

class ScoreboardUpdateEvent(
    full: List<String>,
    val old: List<String>,
) : HanniEvent() {
    val new = full

    val added: List<String> = full - old.toSet()
    val removed: List<String> = old - full.toSet()
}
