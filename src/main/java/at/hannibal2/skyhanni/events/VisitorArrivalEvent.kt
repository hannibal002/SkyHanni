package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures.Visitor

class VisitorArrivalEvent(val visitor: Visitor) : LorenzEvent()