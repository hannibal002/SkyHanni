package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/** Gets posted repeatedly while a key is held down, also on initial key press, use this for holding keys*/
class KeyHeldEvent(val keyCode: Int) : SkyHanniEvent()
