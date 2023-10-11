package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures

class VisitorAcceptEvent(val visitor: GardenVisitorFeatures.Visitor) : LorenzEvent()