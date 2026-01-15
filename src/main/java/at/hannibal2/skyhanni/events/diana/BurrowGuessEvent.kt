package at.hannibal2.skyhanni.events.diana

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.event.diana.GuessEntry

class BurrowGuessEvent(val guess: GuessEntry, val source: String) : SkyHanniEvent()
