package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.SackApi

class SackChangeEvent(
    val sackChanges: List<SackApi.SackChange>,
    val otherItemsAdded: Boolean,
    val otherItemsRemoved: Boolean,
) : HanniEvent()
