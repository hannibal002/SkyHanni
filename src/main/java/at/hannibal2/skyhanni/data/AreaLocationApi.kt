package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.skyblock.AreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object AreaLocationApi {
    var currentArea = AreaType.NONE
        private set

    fun String.isInScoreboardArea(): Boolean = SkyBlockUtils.scoreboardArea == this
    fun String.isInGraphArea(): Boolean = SkyBlockUtils.graphArea == this
    fun String.isInArea(): Boolean = SkyBlockUtils.rawArea == this

    @HandleEvent(priority = HIGHEST)
    private fun onGraphAreaChange() {
        postAreaChangeEvent()
    }

    @HandleEvent(priority = HIGHEST)
    private fun onScoreboardAreaChange() {
        postAreaChangeEvent()
    }

    private fun postAreaChangeEvent() {
        val areaString = SkyBlockUtils.rawArea ?: "???"
        val newArea = AreaType.getByNameOrUnknown(areaString)
        if (newArea == currentArea) return
        val oldArea = currentArea
        currentArea = newArea
        AreaChangeEvent(newArea, oldArea).post()
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Area")
        event.addIrrelevant {
            add("Current Area Identifier: ${currentArea.identifier}")
            add("Current Area displayName: ${currentArea.displayName}")
            add("Raw Area: ${SkyBlockUtils.rawArea ?: "???"}")
            add("Graph Area: ${SkyBlockUtils.graphArea ?: "???"}")
            add("Scoreboard Area: ${SkyBlockUtils.scoreboardArea ?: "???"}")
        }
    }
}
