package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class ScoreboardUpdateEvent(
    @Deprecated("Use new instead", ReplaceWith("new"))
    val full: List<String>,
    val old: List<String>,
) : SkyHanniEvent() {
    val new = full

    val added: List<String> = full - old.toSet()
    val removed: List<String> = old - full.toSet()
}
