package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.milliseconds

object BurrowApi {

    var lastBurrowRelatedChatMessage = SimpleTimeMark.farPast()
    var lastBurrowInteracted: LorenzVec? = null
        private set

    fun setBurrowInteracted(interacted: LorenzVec?) {
        if (interacted != null) {
            GriffinBurrowHelper.addDebug("set last interacted burrow to [${interacted.x}, ${interacted.y}, ${interacted.z}]")
        } else GriffinBurrowHelper.addDebug("set last interacted burrow to null")
        lastBurrowInteracted = interacted
        BurrowWarpHelper.blockWarp(400.milliseconds)
    }
}
