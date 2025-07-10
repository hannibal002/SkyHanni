package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class DrawScreenAfterEvent(context: DrawContext, val mouseX: Int, val mouseY: Int, val ci: CallbackInfo) : RenderingSkyHanniEvent(context)
