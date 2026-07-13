package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread

@Thread(RENDER)
class CharEvent(val keyCode: Int) : SkyHanniEvent()
