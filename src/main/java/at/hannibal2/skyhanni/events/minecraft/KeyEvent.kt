package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

abstract class KeyEvent(open val keyCode: Int) : SkyHanniEvent()
