package at.lorenz.mod.mixinhooks

import at.lorenz.mod.events.GuiRenderItemEvent
import net.minecraft.client.gui.FontRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

val RES_ITEM_GLINT = ResourceLocation("textures/misc/enchanted_item_glint.png")

var skipGlint = false

//fun renderRarity(stack: ItemStack, x: Int, y: Int, ci: CallbackInfo) {
//    if (Utils.inSkyblock && Skytils.config.showItemRarity) {
//        if (mc.currentScreen != null) {
//            if (isStorageMenuActive || isTradeWindowActive || isCustomAHActive) {
//                renderRarity(stack, x, y)
//            }
//        }
//    }
//}

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

//fun renderItemPre(stack: ItemStack, model: IBakedModel, ci: CallbackInfo) {
//    if (!Utils.inSkyblock) return
//    if (stack.item === Items.skull) {
//        val scale = Skytils.config.largerHeadScale.toDouble()
//        GlStateManager.scale(scale, scale, scale)
//    }
//}

//fun modifyGlintRendering(stack: ItemStack, model: IBakedModel, ci: CallbackInfo) {
//    if (Utils.inSkyblock) {
//        val itemId = getSkyBlockItemID(stack)
//        if (GlintCustomizer.glintColors.containsKey(itemId)) {
//            val color = GlintCustomizer.glintColors[itemId]!!.toInt()
//            GlStateManager.depthMask(false)
//            GlStateManager.depthFunc(514)
//            GlStateManager.disableLighting()
//            GlStateManager.blendFunc(768, 1)
//            mc.textureManager.bindTexture(RES_ITEM_GLINT)
//            GlStateManager.matrixMode(5890)
//            GlStateManager.pushMatrix()
//            GlStateManager.scale(8.0f, 8.0f, 8.0f)
//            val f = (Minecraft.getSystemTime() % 3000L).toFloat() / 3000.0f / 8.0f
//            GlStateManager.translate(f, 0.0f, 0.0f)
//            GlStateManager.rotate(-50.0f, 0.0f, 0.0f, 1.0f)
//            (mc.renderItem as AccessorRenderItem).invokeRenderModel(model, color)
//            GlStateManager.popMatrix()
//            GlStateManager.pushMatrix()
//            GlStateManager.scale(8.0f, 8.0f, 8.0f)
//            val f1 = (Minecraft.getSystemTime() % 4873L).toFloat() / 4873.0f / 8.0f
//            GlStateManager.translate(-f1, 0.0f, 0.0f)
//            GlStateManager.rotate(10.0f, 0.0f, 0.0f, 1.0f)
//            (mc.renderItem as AccessorRenderItem).invokeRenderModel(model, color)
//            GlStateManager.popMatrix()
//            GlStateManager.matrixMode(5888)
//            GlStateManager.blendFunc(770, 771)
//            GlStateManager.enableLighting()
//            GlStateManager.depthFunc(515)
//            GlStateManager.depthMask(true)
//            mc.textureManager.bindTexture(TextureMap.locationBlocksTexture)
//            ci.cancel()
//
//            //Since we prematurely exited, we need to reset the matrices
//            GlStateManager.popMatrix()
//        }
//    }
//}