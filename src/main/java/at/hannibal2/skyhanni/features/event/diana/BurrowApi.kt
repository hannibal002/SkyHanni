package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.milliseconds

object BurrowApi {

    var lastBurrowRelatedChatMessage = SimpleTimeMark.farPast()
    var lastBurrowInteracted: LorenzVec? = null

    fun setBurrowInteracted(interacted: LorenzVec?) {
        lastBurrowInteracted = interacted
        BurrowWarpHelper.blockWarp(400.milliseconds)
    }
}
