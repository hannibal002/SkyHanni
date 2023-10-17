package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI

class VisitorAcceptEvent(val visitor: VisitorAPI.Visitor) : LorenzEvent()