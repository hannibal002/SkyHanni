package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class DrawScreenAfterEvent(
    context: GuiGraphicsExtractor,
    val mouseX: Int,
    val mouseY: Int,
    val ci: CallbackInfo,
) : RenderingSkyHanniEvent(context)
