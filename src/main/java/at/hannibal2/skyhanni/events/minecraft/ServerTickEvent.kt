package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ServerTimeMark

@PrimaryFunction("onServerTick")
class ServerTickEvent(val tick: Long) : SkyHanniEvent() {
    val timeMark = ServerTimeMark(tick)
}
