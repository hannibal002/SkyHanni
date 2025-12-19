package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class DrawScreenAfterEvent(context: GuiGraphics, val mouseX: Int, val mouseY: Int, val ci: CallbackInfo) : RenderingSkyHanniEvent(context)
