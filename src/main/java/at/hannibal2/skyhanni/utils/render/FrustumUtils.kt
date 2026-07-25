package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

@SkyHanniModule
object FrustumUtils {

    //~ if < 26.1 'gameRenderer.getMainCamera().getCullFrustum()' -> 'levelRenderer.capturedFrustum'
    private val frustum get() = Minecraft.getInstance().gameRenderer.getMainCamera().getCullFrustum()

    //~ if < 26.1 'frustum.isVisible(box)' -> 'frustum?.isVisible(box) ?: true'
    fun isVisible(box: AABB): Boolean = frustum.isVisible(box)

    fun isVisible(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) =
        isVisible(AABB(minX, minY, minZ, maxX, maxY, maxZ))

}
