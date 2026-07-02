package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

data class ContinuedBlockBreakEvent(val position: LorenzVec, val itemInHand: SafeItemStack?) : SkyHanniEvent()
