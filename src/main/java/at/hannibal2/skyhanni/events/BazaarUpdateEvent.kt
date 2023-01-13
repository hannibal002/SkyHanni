package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.bazaar.BazaarData

class BazaarUpdateEvent(val bazaarMap: MutableMap<String, BazaarData>): LorenzEvent()