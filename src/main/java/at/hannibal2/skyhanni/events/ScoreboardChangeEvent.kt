package at.hannibal2.skyhanni.events

class ScoreboardChangeEvent(val oldList: List<String>, val newList: List<String>) : LorenzEvent()