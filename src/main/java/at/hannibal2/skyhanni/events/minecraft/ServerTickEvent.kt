package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ServerTimeMark

@PrimaryFunction("onServerTick")
class ServerTickEvent(val tick: Int) : SkyHanniEvent() {

    fun isMod(i: Int, offset: Int = 0) = (tick + offset) % i == 0
}
