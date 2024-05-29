package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ArrowType

class QuiverUpdateEvent(val currentArrow: ArrowType?, val currentAmount: Int) : LorenzEvent()
