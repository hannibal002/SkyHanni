package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.utils.compat.DrawContext
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class DrawScreenAfterEvent(context: DrawContext, val mouseX: Int, val mouseY: Int, val ci: CallbackInfo) : RenderingHanniEvent(context)
