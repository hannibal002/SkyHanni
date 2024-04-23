package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EnumPlayerModelParts
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

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

    fun isWearing(entityPlayer: EntityPlayer, parts: EnumPlayerModelParts?): Boolean {
        if (!LorenzUtils.inSkyBlock) return entityPlayer.isWearing(parts)
        return isCoolPerson(entityPlayer.name) || entityPlayer.isWearing(parts)
    }

    fun <T> rotateCorpse(displayName: String, bat: T): Boolean {
        if (isCoolPerson(displayName)) {
            GlStateManager.scale(1.1f, 1.1f, 1.1f)
            GlStateManager.rotate(getRotation(bat).toFloat(), 0f, 1f, 0f)
        }
        return isCoolPerson(displayName)
    }

    fun onIsWearing(entityPlayer: EntityPlayer, cir: CallbackInfoReturnable<Boolean>) {
        if (!isCoolPerson(entityPlayer.name)) return
        GlStateManager.scale(1.1f, 1.1f, 1.1f)
        GlStateManager.rotate(
            getRotation(entityPlayer).toFloat(),
            0f,
            1f,
            0f
        )
        cir.returnValue = true
    }

    fun onEquals(displayName: String, cir: CallbackInfoReturnable<Boolean>) {
        if (isCoolPerson(displayName)) {
            cir.returnValue = true
        }
    }

    private fun isCoolPerson(userName: String?): Boolean {
        if (!LorenzUtils.inSkyBlock) return false
        val name = userName ?: return false
        return ContributorManager.canSpin(name)
    }

    private fun <T> getRotation(entity: T): Int {
        if (entity !is EntityPlayer) return 0
        return (entity.ticksExisted % 90) * 4
    }
}
