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
    fun onGraphAreaChange() {
        postAreaChangeEvent()
    }

    @HandleEvent(priority = HIGHEST)
    fun onScoreboardAreaChange() {
        postAreaChangeEvent()
    }

    fun postAreaChangeEvent() {
        val areaString = SkyBlockUtils.rawArea ?: "???"
        val newArea = AreaType.getByNameOrUnknown(areaString)
        if (newArea == currentArea) return
        val oldArea = currentArea
        currentArea = newArea
        AreaChangeEvent(newArea, oldArea).post()
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Area")
        event.addData("Current Area Identifier: ${currentArea.identifier}")
        event.addData("Current Area displayName: ${currentArea.displayName}")
        event.addData("Graph Area: ${SkyBlockUtils.graphArea ?: "???"}")
        event.addData("Scoreboard Area: ${SkyBlockUtils.scoreboardArea ?: "???"}")
    }
}
