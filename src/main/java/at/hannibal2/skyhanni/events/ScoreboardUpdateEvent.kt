package at.hannibal2.skyhanni.events

class ScoreboardUpdateEvent(
    val old: List<String>,
    val scoreboard: List<String>
) : LorenzEvent() {

    val added: List<String> = scoreboard - old.toSet()
    val removed: List<String> = old - scoreboard.toSet()
}
