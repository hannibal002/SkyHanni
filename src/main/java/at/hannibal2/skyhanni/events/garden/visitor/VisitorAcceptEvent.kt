package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI

// TODO, is both this and VisitorAcceptedEvent needed?
class VisitorAcceptEvent(val visitor: VisitorAPI.Visitor) : LorenzEvent()
