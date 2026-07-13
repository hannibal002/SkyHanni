package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.data.ElectionCandidate

@Thread(DISPATCHER)
class MayorChangeEvent(val old: ElectionCandidate?, val new: ElectionCandidate?, val debug: Boolean = false) : SkyHanniEvent()
