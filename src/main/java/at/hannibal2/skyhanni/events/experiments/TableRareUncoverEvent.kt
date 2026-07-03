package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

// isBook defaults to true so the existing book-detection call site needs no changes
class TableRareUncoverEvent(val dropName: String, val isBook: Boolean = true) : SkyHanniEvent()
