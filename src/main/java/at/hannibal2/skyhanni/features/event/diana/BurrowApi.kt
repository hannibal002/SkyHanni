package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark

object BurrowApi {

    var lastBurrowRelatedChatMessage = SimpleTimeMark.farPast()
    var lastBurrowInteracted: LorenzVec? = null
}
