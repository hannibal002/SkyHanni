package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import net.minecraft.client.gui.FontRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

val RES_ITEM_GLINT = ResourceLocation("textures/misc/enchanted_item_glint.png")

var skipGlint = false

fun renderItemOverlayPost(
    fr: FontRenderer,
    stack: ItemStack?,
    xPosition: Int,
    yPosition: Int,
    text: String?,
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

fun renderItemReturn(stack: ItemStack, x: Int, y: Int, ci: CallbackInfo) {
    RenderRealOverlayEvent(stack, x, y).postAndCatch()
}
