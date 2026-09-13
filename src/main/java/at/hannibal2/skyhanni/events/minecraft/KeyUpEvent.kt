package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InputCode
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.KeyEvent

/** Counterpart to [KeyDownEvent]*/
class KeyUpEvent(val key: InputCode) : SkyHanniEvent() {
    val keyCode: Int get() = key.value
}
