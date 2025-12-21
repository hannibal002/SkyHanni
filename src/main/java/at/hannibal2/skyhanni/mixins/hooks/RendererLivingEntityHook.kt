package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.world.entity.player.Player

object RendererLivingEntityHook {
    private val config get() = SkyHanniMod.feature.dev


    /**
     * Check if the player is on the cool person list and if they should be flipped.
     */
    @JvmStatic
    fun shouldBeUpsideDown(userName: String?): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (!config.flipContributors && !TimeUtils.isAprilFoolsDay) return false
        val name = userName ?: return false
        return ContributorManager.shouldBeUpsideDown(name)
    }

    /**
     * Check if the player should spin and rotate them if the option is on.
     */
    @JvmStatic
    fun rotatePlayer(player: Player): Float? {
        if (!SkyBlockUtils.inSkyBlock) return null
        if (!config.rotateContributors && !TimeUtils.isAprilFoolsDay) return null
        val name = player.name.formattedTextCompatLessResets()
        if (!ContributorManager.shouldSpin(name)) return null
        val rotation = ((player.tickCount % 90) * 4).toFloat()
        return player.yRot + rotation
    }
}
