package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent

/** Gets posted when a key is first pressed, use this for taps*/
class KeyDownEvent(val keyCode: Int) : HanniEvent()
