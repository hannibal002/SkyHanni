package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI

// TODO, is both this and VisitorAcceptEvent needed?
class VisitorAcceptedEvent(val visitor: VisitorAPI.Visitor) : LorenzEvent()
