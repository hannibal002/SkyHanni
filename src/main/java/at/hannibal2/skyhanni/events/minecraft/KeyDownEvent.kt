package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/** Gets posted when a key is first pressed, use this for taps*/
class KeyDownEvent(val keyCode: Int) : SkyHanniEvent()
