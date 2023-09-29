package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase

class RendererLivingEntityHook {
    fun setOutlineColor(red: Float, green: Float, blue: Float, alpha: Float, entity: EntityLivingBase) {
        val color = EntityOutlineRenderer.getCustomOutlineColor(entity)

        if (color != null) {
            val colorRed = (color shr 16 and 255).toFloat() / 255.0f
            val colorGreen = (color shr 8 and 255).toFloat() / 255.0f
            val colorBlue = (color and 255).toFloat() / 255.0f
            GlStateManager.color(colorRed, colorGreen, colorBlue, alpha)
        } else {
            GlStateManager.color(red, green, blue, alpha)
        }
    }
}