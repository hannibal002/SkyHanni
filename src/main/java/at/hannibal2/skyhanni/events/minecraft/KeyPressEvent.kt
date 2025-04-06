package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/** Gets posted each tick it's pressed down*/
class KeyPressEvent(val keyCode: Int) : SkyHanniEvent()
