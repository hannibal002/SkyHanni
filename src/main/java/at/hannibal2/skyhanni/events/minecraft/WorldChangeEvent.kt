package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

object WorldChangeEvent : SkyHanniEvent() {
    override val primaryFunctionName: String = "onWorldChange"
}
