package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.fishing.UsefulSeaCreature

class SeaCreatureFishEvent(val seaCreature: UsefulSeaCreature, val chatEvent: LorenzChatEvent) : LorenzEvent()