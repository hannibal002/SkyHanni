package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EnumPlayerModelParts

class RendererLivingEntityHook {

    // todo use repo list
    private val coolPeople = setOf(
        "Dinnerbone",
        "Biscut",
        "Pinpointed",
        "Berded",
        "Potat_owo",
        "Pnda__",
        "Throwpo",
        "StopUsingSBE",
        "catgirlseraid",
        "ThatGravyBoat",
        "CalMWolfs",
        "Wolfie586"
    )

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

    fun isCoolPerson(name: String?): Boolean {
        if (!LorenzUtils.inSkyBlock) return false
        return coolPeople.contains(name)
    }

    fun isWearing(entityPlayer: EntityPlayer, p_175148_1_: EnumPlayerModelParts?): Boolean {
        if (!LorenzUtils.inSkyBlock) return entityPlayer.isWearing(p_175148_1_)
        return isCoolPerson(entityPlayer.name) || entityPlayer.isWearing(p_175148_1_)
    }

    fun getRotation(entity: Any): Int {
        if (entity !is EntityPlayer) return 0
        return (entity.ticksExisted % 90) * 4
    }
}
