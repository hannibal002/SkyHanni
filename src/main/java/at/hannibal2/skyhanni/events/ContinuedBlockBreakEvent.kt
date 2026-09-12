package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

@PrimaryFunction("onContinuedBlockBreak")
data class ContinuedBlockBreakEvent(val position: LorenzVec, val itemInHand: SafeItemStack?) : SkyHanniEvent()
