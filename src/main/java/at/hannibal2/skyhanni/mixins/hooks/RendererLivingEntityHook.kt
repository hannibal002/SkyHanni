package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

object RendererLivingEntityHook {
    private val config get() = SkyHanniMod.feature.dev

    @JvmStatic
    fun setOutlineColor(red: Float, green: Float, blue: Float, alpha: Float, entity: EntityLivingBase) {
        //#if MC < 1.21
        val color = EntityOutlineRenderer.getCustomOutlineColor(entity)

        if (color != null) {
            val colorRed = (color shr 16 and 255).toFloat() / 255f
            val colorGreen = (color shr 8 and 255).toFloat() / 255f
            val colorBlue = (color and 255).toFloat() / 255f
            GlStateManager.color(colorRed, colorGreen, colorBlue, alpha)
        } else {
            GlStateManager.color(red, green, blue, alpha)
        }
        //#endif
    }

    /**
     * Check if the player is on the cool person list and if they should be flipped.
     */
    @JvmStatic
    fun shouldBeUpsideDown(userName: String?): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (!config.flipContributors && !SkyHanniDebugsAndTests.isAprilFoolsDay) return false
        val name = userName ?: return false
        return ContributorManager.shouldBeUpsideDown(name)
    }

    /**
     * Check if the player should spin and rotate them if the option is on.
     */
    @JvmStatic
    fun rotatePlayer(player: EntityPlayer): Float? {
        if (!SkyBlockUtils.inSkyBlock) return null
        if (!config.rotateContributors && !SkyHanniDebugsAndTests.isAprilFoolsDay) return null
        val name = player.name ?: return null
        if (!ContributorManager.shouldSpin(name)) return null
        val rotation = ((player.ticksExisted % 90) * 4).toFloat()
        //#if MC < 1.21
        GlStateManager.rotate(rotation, 0f, 1f, 0f)
        //#endif
        return player.rotationYaw + rotation
    }
}
