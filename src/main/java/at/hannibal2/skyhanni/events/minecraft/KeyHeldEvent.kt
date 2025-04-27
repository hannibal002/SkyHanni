package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/** Gets posted repeatedly while a key is held down, use this for holding keys
 * only for KEYBOARD. Mouse isn't implemented as it's probably unnecessary*/
class KeyHeldEvent(val keyCode: Int) : SkyHanniEvent()
