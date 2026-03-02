package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.world.entity.player.Player
import java.util.UUID

object RendererLivingEntityHook {
    private val config get() = SkyHanniMod.feature.dev

    /**
     * Check if the player is on the cool person list and if they should be flipped.
     */
    @JvmStatic
    fun shouldBeUpsideDown(uuid: UUID): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (!config.flipContributors && !TimeUtils.isAprilFoolsDay) return false
        return ContributorManager.shouldBeUpsideDown(uuid)
    }

    /**
     * Check if the player should spin and rotate them if the option is on.
     */
    @JvmStatic
    fun rotatePlayer(player: Player): Float? {
        if (!SkyBlockUtils.inSkyBlock) return null
        if (!config.rotateContributors && !TimeUtils.isAprilFoolsDay) return null
        val uuid = player.gameProfile.id
        if (!ContributorManager.shouldSpin(uuid)) return null
        val rotation = ((player.tickCount % 90) * 4).toFloat()
        return player.yRot + rotation
    }
}
