package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.SackAPI

class SackChangeEvent(
    val sackChanges: List<SackAPI.SackChange>,
    val otherItemsAdded: Boolean,
    val otherItemsRemoved: Boolean
) : LorenzEvent()
