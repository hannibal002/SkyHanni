package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class DrawScreenAfterEvent(val context: DrawContext, val mouseX: Int, val mouseY: Int, val ci: CallbackInfo) : SkyHanniEvent()
