package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent

/** Gets posted each tick it's pressed down*/
class KeyPressEvent(val keyCode: Int) : HanniEvent()
