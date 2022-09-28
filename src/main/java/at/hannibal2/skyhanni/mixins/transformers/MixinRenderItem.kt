package at.hannibal2.skyhanni.mixins.transformers

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(RenderItem::class)
abstract class MixinRenderItem {
    @Inject(method = ["renderItemOverlayIntoGUI"], at = [At("RETURN")])
    private fun renderItemOverlayPost(
        fr: FontRenderer,
        stack: ItemStack,
        xPosition: Int,
        yPosition: Int,
        text: String,
        ci: CallbackInfo
    ) {
        GuiRenderItemEvent.RenderOverlayEvent.Post(
            fr,
            stack,
            xPosition,
            yPosition,
            text
        ).postAndCatch()
    }

    @Inject(method = ["renderEffect"], at = [At("HEAD")], cancellable = true)
    fun onRenderEffect(ci: CallbackInfo) {
        ci.cancel()
    }

    @Inject(method = ["renderItemIntoGUI"], at = [At("RETURN")])
    fun renderItemReturn(stack: ItemStack?, x: Int, y: Int, ci: CallbackInfo?) {
        RenderRealOverlayEvent(stack, x, y).postAndCatch()
    }
}