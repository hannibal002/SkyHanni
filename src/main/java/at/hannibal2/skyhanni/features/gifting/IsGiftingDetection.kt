package at.hannibal2.skyhanni.features.gifting

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object IsGiftingDetection {

    private var lastGiftLocation: LorenzVec? = null
    private var lastGiftTime = SimpleTimeMark.farPast()

    fun markLocation() {
        lastGiftLocation = LocationUtils.playerLocation()
        lastGiftTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        lastGiftLocation = null
        lastGiftTime = SimpleTimeMark.farPast()
    }

    fun isCurrentlyGifting(): Boolean = GiftApi.isHoldingGift() || hasPreviouslyGiftedHere()

    private fun hasPreviouslyGiftedHere(): Boolean {
        if (lastGiftTime.passedSince() > 3.minutes) return false

        val lastGiftLocation = lastGiftLocation ?: return false
        if (lastGiftLocation.distanceToPlayer() > 10) return false

        return true
    }
}
