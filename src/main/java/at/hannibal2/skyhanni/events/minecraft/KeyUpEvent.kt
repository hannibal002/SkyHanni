package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import com.mojang.blaze3d.platform.InputConstants

/** Counterpart to [KeyDownEvent]*/
class KeyUpEvent(val key: InputConstants.Key) : SkyHanniEvent() {
    val keyCode: Int get() = key.value
}
