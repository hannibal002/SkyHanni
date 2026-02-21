package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.SimpleTimeMark

class VisitorNextArrivalChangeEvent(val nextVisitor: SimpleTimeMark) : SkyHanniEvent()
