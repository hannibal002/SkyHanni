package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.events.LorenzEvent

class WardrobeUpdateEvent(var new: List<WardrobeAPI.WardrobeData?>, var old: List<WardrobeAPI.WardrobeData?>) :
    LorenzEvent()
