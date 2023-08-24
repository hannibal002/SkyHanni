package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.inventory.SackAPI

class SackChangeEvent(val sackChanges: List<SackAPI.SackChange>) : LorenzEvent()
