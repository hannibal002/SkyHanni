package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

/** Gets posted when a key is first pressed, use this for taps*/
@Thread(RENDER)
class KeyDownEvent(val keyCode: Int) : SkyHanniEvent()
