package at.hannibal2.hanni.events.garden.visitor

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.garden.visitor.VisitorApi.Visitor

class VisitorArrivalEvent(val visitor: Visitor) : HanniEvent()
