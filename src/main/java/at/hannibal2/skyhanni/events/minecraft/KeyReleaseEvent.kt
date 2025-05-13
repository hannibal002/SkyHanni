package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/** Gets posted when a key is released, counterpart to [KeyPressEvent]*/
class KeyReleaseEvent(val keyCode: Int) : SkyHanniEvent()
