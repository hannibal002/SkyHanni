package at.hannibal2.skyhanni.mixinhooks

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
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